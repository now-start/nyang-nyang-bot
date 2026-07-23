package org.nowstart.nyangnyangbot.application.service.google;

import io.micrometer.common.util.StringUtils;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.google.SyncGoogleSheetUseCase;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort.GoogleSheetRow;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleSheetService implements SyncGoogleSheetUseCase {

    private final GoogleSheetPointBatchApplier pointBatchApplier;
    private final GoogleSheetPort googleSheetPort;

    // 한 인스턴스에서 스케줄러와 수동 API가 같은 시트를 중복 조회하지 않도록 실행을 합친다.
    // 사용자별 정합성은 DB 잠금 기반 reconcile이 보장하며, 이 플래그는 다중 인스턴스 singleton은 아니다.
    private final AtomicBoolean syncing = new AtomicBoolean(false);

    @Override
    public void synchronizePoints() {
        if (!syncing.compareAndSet(false, true)) {
            log.warn("[DBSync] 이미 동기화가 진행 중이어서 이번 실행을 건너뜁니다.");
            return;
        }
        try {
            List<GoogleSheetRow> googleSheetRows = getSheetValues();
            pointBatchApplier.apply(googleSheetRows);
        } finally {
            syncing.set(false);
        }
    }

    List<GoogleSheetRow> getSheetValues() {
        return normalizeRows(googleSheetPort.readPointRows());
    }

    List<GoogleSheetRow> normalizeRows(List<GoogleSheetRow> rows) {
        return rows.stream()
                .filter(row -> row != null && !StringUtils.isBlank(row.userId()) && row.point() != null)
                .collect(Collectors.toMap(
                        GoogleSheetRow::userId,
                        row -> row,
                        (existing, replacement) -> replacement
                )).values().stream()
                .toList();
    }
}
