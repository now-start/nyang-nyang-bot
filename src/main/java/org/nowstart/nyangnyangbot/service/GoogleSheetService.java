package org.nowstart.nyangnyangbot.service;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.io.FileInputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.sheet.GoogleSheetDto;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;
import org.nowstart.nyangnyangbot.data.entity.UserKarmaEntity;
import org.nowstart.nyangnyangbot.data.property.GoogleProperty;
import org.nowstart.nyangnyangbot.data.type.FavoriteHistoryType;
import org.nowstart.nyangnyangbot.data.type.FavoriteKarmaSourceType;
import org.nowstart.nyangnyangbot.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.nowstart.nyangnyangbot.repository.UserKarmaRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GoogleSheetService {

    private static final String RANGE = "호감도 순위표!B2:H2000";
    private static final String SYNC_LABEL = "데이터 동기화";
    private final GoogleProperty googleProperty;
    private final FavoriteRepository favoriteRepository;
    private final UserKarmaRepository userKarmaRepository;
    private final FavoriteHistoryRepository favoriteHistoryRepository;
    private final FavoriteAggregationService favoriteAggregationService;

    public void updateFavorite() {
        List<GoogleSheetDto> googleSheetDtoList = getSheetValues();

        for (GoogleSheetDto dto : googleSheetDtoList) {
            FavoriteEntity favoriteEntity = favoriteRepository.findById(dto.userId())
                    .orElseGet(() -> favoriteRepository.save(FavoriteEntity.builder()
                            .userId(dto.userId())
                            .nickName(dto.nickName())
                            .totalFavorite(0)
                            .karmaScore(0)
                            .attendanceCount(0)
                            .build()));

            if (!Objects.equals(favoriteEntity.getNickName(), dto.nickName())) {
                favoriteEntity.setNickName(dto.nickName());
            }

            int before = favoriteEntity.safeFavorite();
            syncFavoriteFromSheet(favoriteEntity, dto.favorite());
            FavoriteAggregationService.FavoriteTotals totals = favoriteAggregationService.recalculate(favoriteEntity);
            if (before != totals.totalFavorite()) {
                favoriteHistoryRepository.save(FavoriteHistoryEntity.builder()
                        .favoriteEntity(favoriteEntity)
                        .history(SYNC_LABEL)
                        .favorite(totals.totalFavorite())
                        .karmaScore(totals.karmaScore())
                        .attendanceCount(totals.attendanceCount())
                        .type(FavoriteHistoryType.SYNC)
                        .build());
            }
        }
    }

    @SneakyThrows
    List<GoogleSheetDto> getSheetValues() {
        GoogleCredentials credentials =
                GoogleCredentials.fromStream(new FileInputStream(googleProperty.key())).createScoped(SheetsScopes.SPREADSHEETS);
        Sheets sheets = new Sheets.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
                .setApplicationName("google-sheet-project")
                .build();

        List<List<Object>> values = sheets.spreadsheets()
                .values()
                .get(googleProperty.id(), RANGE)
                .execute().getValues();

        return normalizeRows(values.stream()
                .map(value -> {
                    log.info("[GoogleSheet] Row value: {}", value);
                    return GoogleSheetDto.fromRow(value);
                })
                .toList());
    }

    List<GoogleSheetDto> normalizeRows(List<GoogleSheetDto> rows) {
        return rows.stream()
                .filter(dto -> dto != null && !StringUtils.isBlank(dto.userId()) && dto.favorite() != null)
                .collect(Collectors.toMap(
                        GoogleSheetDto::userId,
                        dto -> dto,
                        (existing, replacement) -> replacement
                )).values().stream()
                .toList();
    }

    private void syncFavoriteFromSheet(FavoriteEntity favoriteEntity, Integer desiredFavorite) {
        userKarmaRepository.deleteAllByFavoriteEntityUserIdAndSourceType(
                favoriteEntity.getUserId(),
                FavoriteKarmaSourceType.SYNC
        );
        int attendanceCount = favoriteEntity.safeAttendanceCount();
        int nonSyncKarmaScore = favoriteAggregationService.calculateNonSyncKarmaScore(favoriteEntity.getUserId());
        int syncAmount = desiredFavorite - attendanceCount - nonSyncKarmaScore;
        if (syncAmount == 0) {
            return;
        }
        userKarmaRepository.save(UserKarmaEntity.builder()
                .favoriteEntity(favoriteEntity)
                .quantity(1)
                .label(SYNC_LABEL)
                .amount(syncAmount)
                .sourceType(FavoriteKarmaSourceType.SYNC)
                .build());
    }
}
