package org.nowstart.nyangnyangbot.application.service.google;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.point.ReconcilePointBalanceUseCase.ReconcilePointBalanceCommand;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort.GoogleSheetRow;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class GoogleSheetServiceTest {

    @Mock
    private GoogleSheetPointBatchApplier pointBatchApplier;
    @Mock
    private GoogleSheetPort googleSheetPort;

    @Test
    void synchronizePoints_ReconcilesSheetValueAgainstLedgerSum() {
        GoogleSheetService service = org.mockito.Mockito.spy(new GoogleSheetService(
                pointBatchApplier,
                googleSheetPort
        ));
        org.mockito.Mockito.doReturn(List.of(new GoogleSheetRow("치즈냥", "user-1", 70L)))
                .when(service).getSheetValues();

        service.synchronizePoints();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GoogleSheetRow>> captor = ArgumentCaptor.forClass(List.class);
        verify(pointBatchApplier).apply(captor.capture());
        then(captor.getValue()).containsExactly(new GoogleSheetRow("치즈냥", "user-1", 70L));
    }

    @Test
    void synchronizePoints_DelegatesEvenUnchangedTargetSoLockAndBalanceReadStayAtomic() {
        GoogleSheetService service = org.mockito.Mockito.spy(new GoogleSheetService(
                pointBatchApplier,
                googleSheetPort
        ));
        org.mockito.Mockito.doReturn(List.of(new GoogleSheetRow("치즈냥", "user-1", 50L)))
                .when(service).getSheetValues();

        service.synchronizePoints();

        verify(pointBatchApplier).apply(anyList());
    }

    @Test
    void normalizeRows_KeepsLatestDuplicate() {
        GoogleSheetService service = new GoogleSheetService(
                pointBatchApplier,
                googleSheetPort
        );

        then(service.normalizeRows(List.of(
                new GoogleSheetRow("이전", "user-1", 30L),
                new GoogleSheetRow("최신", "user-1", 80L)
        ))).containsExactly(new GoogleSheetRow("최신", "user-1", 80L));
    }

    @Test
    void batchApplierRunsAllReconciliationsInOneRequiredTransaction() throws NoSuchMethodException {
        var reconcile = org.mockito.Mockito.mock(
                org.nowstart.nyangnyangbot.application.port.in.point.ReconcilePointBalanceUseCase.class
        );
        GoogleSheetPointBatchApplier applier = new GoogleSheetPointBatchApplier(reconcile);

        applier.apply(List.of(new GoogleSheetRow("치즈냥", "user-1", 70L)));

        ArgumentCaptor<ReconcilePointBalanceCommand> captor =
                ArgumentCaptor.forClass(ReconcilePointBalanceCommand.class);
        verify(reconcile).reconcileToBalance(captor.capture());
        then(captor.getValue().targetBalance()).isEqualTo(70);
        then(captor.getValue().sourceReference()).isEqualTo("google-sheet");
        then(captor.getValue().description()).isEqualTo("구글 시트 동기화");
        then(captor.getValue().createIfMissing()).isTrue();
        then(GoogleSheetPointBatchApplier.class.getMethod("apply", List.class)
                .getAnnotation(Transactional.class)).isNotNull();
    }
}
