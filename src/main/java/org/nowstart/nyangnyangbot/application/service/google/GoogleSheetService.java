package org.nowstart.nyangnyangbot.application.service.google;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
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

    // 스케줄러(매일 04:00)와 수동 동기화 API가 동시에 실행되면 같은 delta가 두 번 적용되어
    // 호감도 원장이 이중 기록될 수 있으므로 단일 실행을 보장한다.
    // 단일 인스턴스 운영을 전제로 하며, 다중 인스턴스로 확장 시 분산 락(ShedLock 등)으로 대체해야 한다.
    private final AtomicBoolean syncing = new AtomicBoolean(false);

    @Override
    public void updateFavorite() {
        if (!syncing.compareAndSet(false, true)) {
            log.warn("[DBSync] 이미 동기화가 진행 중이어서 이번 실행을 건너뜁니다.");
            return;
        }
        try {
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
        } finally {
            syncing.set(false);
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
