package org.nowstart.nyangnyangbot.application.service.google;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.google.SyncGoogleSheetUseCase;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort.GoogleSheetRow;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort.SummaryResult;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GoogleSheetService implements SyncGoogleSheetUseCase {

    private final FavoriteQueryPort favoriteQueryPort;
    private final AdjustFavoriteUseCase adjustFavoriteUseCase;
    private final GoogleSheetPort googleSheetPort;

    @Override
    public void updateFavorite() {
        List<GoogleSheetRow> googleSheetRows = getSheetValues();

        for (GoogleSheetRow row : googleSheetRows) {
            SummaryResult favorite = favoriteQueryPort.getOrCreate(row.userId(), row.nickName());

            if (!Objects.equals(favorite.nickName(), row.nickName())) {
                favoriteQueryPort.updateNickName(row.userId(), row.nickName());
            }

            if (!Objects.equals(favorite.favorite(), row.favorite())) {
                int before = favorite.favorite() == null ? 0 : favorite.favorite();
                adjustFavoriteUseCase.adjust(AdjustFavoriteCommand.builder()
                        .userId(row.userId())
                        .nickName(row.nickName())
                        .delta(row.favorite() - before)
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

    List<GoogleSheetRow> getSheetValues() {
        return normalizeRows(googleSheetPort.readFavoriteRows());
    }

    List<GoogleSheetRow> normalizeRows(List<GoogleSheetRow> rows) {
        return rows.stream()
                .filter(row -> row != null && !StringUtils.isBlank(row.userId()) && row.favorite() != null)
                .collect(Collectors.toMap(
                        GoogleSheetRow::userId,
                        row -> row,
                        (existing, replacement) -> replacement
                )).values().stream()
                .toList();
    }
}
