package org.nowstart.nyangnyangbot.application.service.google;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.point.ReconcilePointBalanceUseCase;
import org.nowstart.nyangnyangbot.application.port.in.point.ReconcilePointBalanceUseCase.ReconcilePointBalanceCommand;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort.GoogleSheetRow;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GoogleSheetPointBatchApplier {

    private final ReconcilePointBalanceUseCase reconcilePointBalanceUseCase;

    @Transactional
    public void apply(List<GoogleSheetRow> rows) {
        for (GoogleSheetRow row : rows) {
            reconcilePointBalanceUseCase.reconcileToBalance(ReconcilePointBalanceCommand.builder()
                    .userId(row.userId())
                    .displayName(row.displayName())
                    .targetBalance(row.point())
                    .sourceReference("google-sheet")
                    .description("데이터 동기화")
                    .createIfMissing(true)
                    .build());
        }
    }
}
