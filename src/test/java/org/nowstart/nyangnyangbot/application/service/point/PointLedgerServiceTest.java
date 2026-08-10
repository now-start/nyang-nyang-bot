package org.nowstart.nyangnyangbot.application.service.point;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase.AdjustPointCommand;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase.PointLedgerResult;
import org.nowstart.nyangnyangbot.application.port.out.persistence.PersistenceFailureClassifierPort;
import org.nowstart.nyangnyangbot.application.port.out.transaction.TransactionContextPort;
import org.nowstart.nyangnyangbot.application.service.point.PointLedgerTransactionExecutor.WriteRequest;
import org.nowstart.nyangnyangbot.domain.point.PointSourceType;

@ExtendWith(MockitoExtension.class)
class PointLedgerServiceTest {

    @Mock
    private PointLedgerTransactionExecutor transactionExecutor;

    @Mock
    private PersistenceFailureClassifierPort persistenceFailureClassifierPort;

    @Mock
    private TransactionContextPort transactionContextPort;

    @InjectMocks
    private PointLedgerService service;

    @Test
    void adjust_NormalizesReplaySensitiveTextBeforeTransactionalWrite() {
        given(transactionExecutor.execute(org.mockito.ArgumentMatchers.any())).willReturn(new PointLedgerResult(5L));

        service.adjust(command());

        ArgumentCaptor<WriteRequest> captor = ArgumentCaptor.forClass(WriteRequest.class);
        org.mockito.BDDMockito.then(transactionExecutor).should().execute(captor.capture());
        then(captor.getValue().description()).isEqualTo("보상");
        then(captor.getValue().privateNote()).isEqualTo("감사 메모");
        then(captor.getValue().actorUserId()).isEqualTo("admin-1");
    }

    @Test
    void adjust_AfterUniqueRaceResolvesExistingEntryWithoutRetryingWrite() {
        RuntimeException race = new IllegalStateException("duplicate key");
        PointLedgerResult duplicate = new PointLedgerResult(5L);
        given(transactionExecutor.execute(org.mockito.ArgumentMatchers.any())).willThrow(race);
        given(persistenceFailureClassifierPort.isConflict(race)).willReturn(true);
        given(transactionExecutor.resolveDuplicate(org.mockito.ArgumentMatchers.any()))
                .willReturn(Optional.of(duplicate));

        PointLedgerResult result = service.adjust(command());

        then(result).isEqualTo(duplicate);
        org.mockito.BDDMockito.then(transactionExecutor).should().execute(org.mockito.ArgumentMatchers.any());
        org.mockito.BDDMockito.then(transactionExecutor).should()
                .resolveDuplicate(org.mockito.ArgumentMatchers.any());
        org.mockito.BDDMockito.then(transactionExecutor).shouldHaveNoMoreInteractions();
    }

    @Test
    void adjust_WhenUniqueFailureHasNoMatchingKeyRethrowsOriginalFailure() {
        RuntimeException race = new IllegalStateException("other constraint");
        given(transactionExecutor.execute(org.mockito.ArgumentMatchers.any())).willThrow(race);
        given(persistenceFailureClassifierPort.isConflict(race)).willReturn(true);
        given(transactionExecutor.resolveDuplicate(org.mockito.ArgumentMatchers.any()))
                .willReturn(Optional.empty());

        thenThrownBy(() -> service.adjust(command())).isSameAs(race);
    }

    @Test
    void adjust_WhenJoiningAggregateTransactionDoesNotHideFailureOrReadFromRollbackOnlyTransaction() {
        RuntimeException race = new IllegalStateException("duplicate key");
        given(transactionExecutor.execute(org.mockito.ArgumentMatchers.any())).willThrow(race);
        given(persistenceFailureClassifierPort.isConflict(race)).willReturn(true);
        given(transactionContextPort.isTransactionActive()).willReturn(true);

        thenThrownBy(() -> service.adjust(command())).isSameAs(race);

        org.mockito.BDDMockito.then(transactionExecutor).should(org.mockito.Mockito.never())
                .resolveDuplicate(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void adjust_WhenFailureIsNotConflictRethrowsWithoutResolvingDuplicate() {
        RuntimeException failure = new IllegalStateException("unavailable");
        given(transactionExecutor.execute(org.mockito.ArgumentMatchers.any())).willThrow(failure);

        thenThrownBy(() -> service.adjust(command())).isSameAs(failure);

        org.mockito.BDDMockito.then(transactionExecutor).should(org.mockito.Mockito.never())
                .resolveDuplicate(org.mockito.ArgumentMatchers.any());
    }

    private AdjustPointCommand command() {
        return AdjustPointCommand.builder()
                .userId("user-1")
                .displayName("냥이")
                .delta(7)
                .sourceType(PointSourceType.REWARD_MANUAL)
                .sourceReference("reward:1")
                .description("  보상  ")
                .privateNote("  감사 메모  ")
                .actorUserId("  admin-1  ")
                .idempotencyKey("reward:1")
                .createIfMissing(true)
                .build();
    }
}
