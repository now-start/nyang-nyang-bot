package org.nowstart.nyangnyangbot.application.service.point;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.nowstart.nyangnyangbot.application.port.out.point.PointLedgerPort;
import org.nowstart.nyangnyangbot.application.port.out.point.PointLedgerPort.AppendPointEntry;
import org.nowstart.nyangnyangbot.application.port.out.point.PointLedgerPort.LedgerEntryRecord;
import org.nowstart.nyangnyangbot.application.service.point.PointLedgerTransactionExecutor.WriteRequest;
import org.nowstart.nyangnyangbot.application.service.point.PointLedgerTransactionExecutor.ReconcileRequest;
import org.nowstart.nyangnyangbot.domain.point.PointSourceType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class PointLedgerTransactionExecutorTest {

    @Test
    void execute_LocksUserAndDerivesBalanceOnlyFromLedgerSum() {
        PointLedgerPort port = Mockito.mock(PointLedgerPort.class);
        PointLedgerTransactionExecutor executor = new PointLedgerTransactionExecutor(port);
        given(port.lockUser("user-1", "냥이", true)).willReturn(true);
        given(port.balance("user-1")).willReturn(10L);
        given(port.findByIdempotencyKey("reward:1")).willReturn(Optional.empty());
        given(port.append(org.mockito.ArgumentMatchers.any())).willReturn(entry());

        var result = executor.execute(request());

        then(result.ledgerId()).isEqualTo(5L);
        ArgumentCaptor<AppendPointEntry> captor = ArgumentCaptor.forClass(AppendPointEntry.class);
        Mockito.verify(port).append(captor.capture());
        then(captor.getValue().sourceReference()).isEqualTo("source:1");
        then(captor.getValue().privateNote()).isEqualTo("감사 메모");
        then(captor.getValue().actorUserId()).isEqualTo("admin-1");
    }

    @Test
    void execute_ExactReplayReturnsDuplicate() {
        PointLedgerPort port = Mockito.mock(PointLedgerPort.class);
        PointLedgerTransactionExecutor executor = new PointLedgerTransactionExecutor(port);
        given(port.lockUser("user-1", "냥이", true)).willReturn(true);
        given(port.findByIdempotencyKey("reward:1")).willReturn(Optional.of(entry()));

        var result = executor.execute(request());

        then(result.ledgerId()).isEqualTo(5L);
        Mockito.verify(port, Mockito.never()).balance(Mockito.anyString());
        Mockito.verify(port, Mockito.never()).append(org.mockito.ArgumentMatchers.any());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("conflictingEntries")
    void execute_ReplayMustMatchEverySemanticLedgerField(String field, LedgerEntryRecord existing) {
        PointLedgerPort port = Mockito.mock(PointLedgerPort.class);
        PointLedgerTransactionExecutor executor = new PointLedgerTransactionExecutor(port);
        given(port.lockUser("user-1", "냥이", true)).willReturn(true);
        given(port.balance("user-1")).willReturn(17L);
        given(port.findByIdempotencyKey("reward:1")).willReturn(Optional.of(existing));

        thenThrownBy(() -> executor.execute(request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("point idempotency key conflicts with existing entry");
    }

    @Test
    void correct_RejectsCorrectionTargetOwnedByAnotherUser() {
        PointLedgerPort port = Mockito.mock(PointLedgerPort.class);
        PointLedgerTransactionExecutor executor = new PointLedgerTransactionExecutor(port);
        given(port.lockUser("user-1", null, false)).willReturn(true);
        given(port.balance("user-1")).willReturn(10L);
        given(port.findByIdempotencyKey("correction:3")).willReturn(Optional.empty());
        given(port.findCorrectionTargetForUpdate(3L)).willReturn(Optional.of(new LedgerEntryRecord(
                3L,
                "user-2",
                5L,
                PointSourceType.ADMIN_ADJUSTMENT,
                null,
                "조정",
                null,
                null,
                null
        )));

        WriteRequest correction = new WriteRequest(
                "user-1", null, -5, PointSourceType.CORRECTION, null, "포인트 정정", null,
                3L, null, "correction:3", true, false
        );
        thenThrownBy(() -> executor.execute(correction))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Correction target must belong to the same user");
    }

    @Test
    void ledgerOperationsUseDefaultPropagationAndDuplicateResolutionIsReadOnly() throws Exception {
        Transactional write = PointLedgerTransactionExecutor.class
                .getMethod("execute", WriteRequest.class)
                .getAnnotation(Transactional.class);
        Transactional reconcile = PointLedgerTransactionExecutor.class
                .getMethod("reconcile", ReconcileRequest.class)
                .getAnnotation(Transactional.class);
        Transactional resolution = PointLedgerTransactionExecutor.class
                .getMethod("resolveDuplicate", WriteRequest.class)
                .getAnnotation(Transactional.class);

        then(write.propagation()).isEqualTo(Propagation.REQUIRED);
        then(reconcile.propagation()).isEqualTo(Propagation.REQUIRED);
        then(resolution.propagation()).isEqualTo(Propagation.REQUIRED);
        then(resolution.readOnly()).isTrue();
    }

    @Test
    void reconcile_LocksUserBeforeReadingBalanceAndAppendsExactDifference() {
        PointLedgerPort port = Mockito.mock(PointLedgerPort.class);
        PointLedgerTransactionExecutor executor = new PointLedgerTransactionExecutor(port);
        given(port.lockUser("user-1", "냥이", true)).willReturn(true);
        given(port.balance("user-1")).willReturn(50L);
        given(port.append(org.mockito.ArgumentMatchers.any())).willReturn(entry(
                "user-1", 20, PointSourceType.GOOGLE_SHEET_SYNC, "google-sheet", "구글 시트 동기화",
                null, null, null
        ));

        var result = executor.reconcile(reconcileRequest(70L));

        org.mockito.InOrder order = Mockito.inOrder(port);
        order.verify(port).lockUser("user-1", "냥이", true);
        order.verify(port).balance("user-1");
        ArgumentCaptor<AppendPointEntry> captor = ArgumentCaptor.forClass(AppendPointEntry.class);
        order.verify(port).append(captor.capture());
        then(captor.getValue().delta()).isEqualTo(20L);
        then(captor.getValue().sourceType()).isEqualTo(PointSourceType.GOOGLE_SHEET_SYNC);
        then(result.ledgerId()).isEqualTo(5L);
    }

    @Test
    void reconcile_WhenAlreadyAtTargetDoesNotAppendLedgerEntry() {
        PointLedgerPort port = Mockito.mock(PointLedgerPort.class);
        PointLedgerTransactionExecutor executor = new PointLedgerTransactionExecutor(port);
        given(port.lockUser("user-1", "냥이", true)).willReturn(true);
        given(port.balance("user-1")).willReturn(70L);

        var result = executor.reconcile(reconcileRequest(70L));

        then(result.ledgerId()).isNull();
        Mockito.verify(port, Mockito.never()).append(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reconcile_UsesExactSubtractionForOverflowSafety() {
        PointLedgerPort port = Mockito.mock(PointLedgerPort.class);
        PointLedgerTransactionExecutor executor = new PointLedgerTransactionExecutor(port);
        given(port.lockUser("user-1", "냥이", true)).willReturn(true);
        given(port.balance("user-1")).willReturn(-1L);

        thenThrownBy(() -> executor.reconcile(reconcileRequest(Long.MAX_VALUE)))
                .isInstanceOf(ArithmeticException.class);
        Mockito.verify(port, Mockito.never()).append(org.mockito.ArgumentMatchers.any());
    }

    private static Stream<Arguments> conflictingEntries() {
        return Stream.of(
                Arguments.of("userId", entry("user-2", 7, PointSourceType.REWARD_MANUAL, "source:1", "보상",
                        "감사 메모", null, "admin-1")),
                Arguments.of("delta", entry("user-1", 8, PointSourceType.REWARD_MANUAL, "source:1", "보상",
                        "감사 메모", null, "admin-1")),
                Arguments.of("sourceType", entry("user-1", 7, PointSourceType.ADMIN_ADJUSTMENT, "source:1", "보상",
                        "감사 메모", null, "admin-1")),
                Arguments.of("sourceReference", entry("user-1", 7, PointSourceType.REWARD_MANUAL, "source:2", "보상",
                        "감사 메모", null, "admin-1")),
                Arguments.of("description", entry("user-1", 7, PointSourceType.REWARD_MANUAL, "source:1", "다른 보상",
                        "감사 메모", null, "admin-1")),
                Arguments.of("privateNote", entry("user-1", 7, PointSourceType.REWARD_MANUAL, "source:1", "보상",
                        "다른 메모", null, "admin-1")),
                Arguments.of("correction", entry("user-1", 7, PointSourceType.REWARD_MANUAL, "source:1", "보상",
                        "감사 메모", 3L, "admin-1")),
                Arguments.of("actor", entry("user-1", 7, PointSourceType.REWARD_MANUAL, "source:1", "보상",
                        "감사 메모", null, "admin-2"))
        );
    }

    private static LedgerEntryRecord entry() {
        return entry("user-1", 7, PointSourceType.REWARD_MANUAL, "source:1", "보상", "감사 메모", null,
                "admin-1");
    }

    private static LedgerEntryRecord entry(
            String userId,
            long delta,
            PointSourceType sourceType,
            String sourceReference,
            String description,
            String privateNote,
            Long correctionId,
            String actorUserId
    ) {
        return new LedgerEntryRecord(
                5L, userId, delta, sourceType, sourceReference, description, privateNote, correctionId, actorUserId
        );
    }

    private WriteRequest request() {
        return new WriteRequest(
                "user-1", "냥이", 7, PointSourceType.REWARD_MANUAL, "source:1", "보상", "감사 메모", null,
                "admin-1", "reward:1", false, true
        );
    }

    private ReconcileRequest reconcileRequest(long targetBalance) {
        return new ReconcileRequest(
                "user-1", "냥이", targetBalance, "google-sheet", "구글 시트 동기화", null, null,
                "point:random", true
        );
    }
}
