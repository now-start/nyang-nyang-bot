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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.sheet.GoogleSheetDto;
import org.nowstart.nyangnyangbot.data.entity.ChannelEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;
import org.nowstart.nyangnyangbot.data.property.GoogleProperty;
import org.nowstart.nyangnyangbot.repository.FavoriteHistoryRepository;
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
    private final FavoriteHistoryRepository favoriteHistoryRepository;
    private final ChannelService channelService;

    public void updateFavorite() {
        List<GoogleSheetDto> googleSheetDtoList = getSheetValues();
        ChannelEntity ownerChannel = channelService.getDefaultChannel();

        for (GoogleSheetDto dto : googleSheetDtoList) {
            ChannelEntity targetChannel = channelService.getOrCreate(dto.userId(), dto.nickName());
            if (targetChannel == null) {
                continue;
            }
            FavoriteEntity favoriteEntity = favoriteRepository
                    .findByOwnerChannelIdAndTargetChannelId(ownerChannel.getId(), targetChannel.getId())
                .orElseGet(() -> favoriteRepository.save(FavoriteEntity.builder()
                        .ownerChannel(ownerChannel)
                        .targetChannel(targetChannel)
                        .favorite(0)
                        .build()));

            if (!favoriteEntity.getFavorite().equals(dto.favorite())) {
                favoriteEntity.setNickName(dto.nickName());
                favoriteEntity.setFavorite(dto.favorite());
                favoriteHistoryRepository.save(FavoriteHistoryEntity.builder()
                        .favorite(favoriteEntity)
                    .history("데이터 동기화")
                        .favoriteValue(dto.favorite())
                    .build());
            }
        }
    }

    @SneakyThrows
    private List<GoogleSheetDto> getSheetValues() {
        GoogleCredentials credentials =
                GoogleCredentials.fromStream(new FileInputStream(googleProperty.key())).createScoped(SheetsScopes.SPREADSHEETS);
        Sheets sheets = new Sheets.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
            .setApplicationName("google-sheet-project")
            .build();

        List<List<Object>> values = sheets.spreadsheets()
            .values()
                .get(googleProperty.id(), RANGE)
            .execute().getValues();

        return values.stream()
                .map(value -> {
                    log.info("[GoogleSheet] Row value: {}", value);
                    return GoogleSheetDto.fromRow(value);
                })
                .filter(dto -> dto != null && !StringUtils.isBlank(dto.userId()))
                .collect(Collectors.toMap(
                        GoogleSheetDto::userId,
                        dto -> dto,
                        (existing, replacement) -> replacement
                )).values().stream()
                .toList();
    }
}
