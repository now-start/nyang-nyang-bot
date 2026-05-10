package org.nowstart.nyangnyangbot.application.service;

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
import org.nowstart.nyangnyangbot.domain.model.FavoriteSummary;
import org.nowstart.nyangnyangbot.application.favorite.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.favorite.AdjustFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.gateway.out.favorite.FavoriteQueryPort;
import org.nowstart.nyangnyangbot.application.dto.sheet.GoogleSheetDto;
import org.nowstart.nyangnyangbot.config.property.GoogleProperty;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GoogleSheetService {

    private static final String RANGE = "호감도 순위표!B2:H2000";
    private final GoogleProperty googleProperty;
    private final FavoriteQueryPort favoriteQueryPort;
    private final AdjustFavoriteUseCase adjustFavoriteUseCase;

    public void updateFavorite() {
        List<GoogleSheetDto> googleSheetDtoList = getSheetValues();

        for (GoogleSheetDto dto : googleSheetDtoList) {
            FavoriteSummary favorite = favoriteQueryPort.getOrCreate(dto.userId(), dto.nickName());

            if (!Objects.equals(favorite.nickName(), dto.nickName())) {
                favoriteQueryPort.updateNickName(dto.userId(), dto.nickName());
            }

            if (!Objects.equals(favorite.favorite(), dto.favorite())) {
                int before = favorite.favorite() == null ? 0 : favorite.favorite();
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
