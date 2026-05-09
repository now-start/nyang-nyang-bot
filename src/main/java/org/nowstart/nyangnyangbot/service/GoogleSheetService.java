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
import org.nowstart.nyangnyangbot.application.favorite.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.favorite.AdjustFavoriteUseCase;
import org.nowstart.nyangnyangbot.data.dto.sheet.GoogleSheetDto;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.property.GoogleProperty;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GoogleSheetService {

    private static final String RANGE = "호감도 순위표!B2:H2000";
    private final GoogleProperty googleProperty;
    private final FavoriteRepository favoriteRepository;
    private final AdjustFavoriteUseCase adjustFavoriteUseCase;

    public void updateFavorite() {
        List<GoogleSheetDto> googleSheetDtoList = getSheetValues();

        for (GoogleSheetDto dto : googleSheetDtoList) {
            FavoriteEntity favoriteEntity = favoriteRepository.findById(dto.userId())
                    .orElseGet(() -> favoriteRepository.save(FavoriteEntity.builder()
                        .userId(dto.userId())
                        .nickName(dto.nickName())
                            .favorite(0)
                            .build()));

            if (!Objects.equals(favoriteEntity.getNickName(), dto.nickName())) {
                favoriteEntity.setNickName(dto.nickName());
            }

            if (!Objects.equals(favoriteEntity.getFavorite(), dto.favorite())) {
                int before = favoriteEntity.getFavorite() == null ? 0 : favoriteEntity.getFavorite();
                adjustFavoriteUseCase.adjust(AdjustFavoriteCommand.builder()
                        .userId(dto.userId())
                        .nickName(dto.nickName())
                        .delta(dto.favorite() - before)
                        .sourceType(FavoriteSourceType.SHEET_MIGRATION)
                        .sourceId("google-sheet")
                        .displayCategory("MIGRATION")
                        .publicDescription("데이터 동기화")
                        .allowNegativeBalance(true)
                        .createIfMissing(true)
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
}
