package org.nowstart.nyangnyangbot.application.service.point;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase;
import org.nowstart.nyangnyangbot.application.port.in.point.CorrectPointLedgerUseCase;
import org.nowstart.nyangnyangbot.application.port.in.point.GrantPointUseCase;
import org.nowstart.nyangnyangbot.application.port.in.point.ReconcilePointBalanceUseCase;
import org.nowstart.nyangnyangbot.application.service.point.PointLedgerTransactionExecutor.WriteRequest;
import org.nowstart.nyangnyangbot.application.service.point.PointLedgerTransactionExecutor.ReconcileRequest;
import org.nowstart.nyangnyangbot.domain.point.PointSourceType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class PointLedgerService implements AdjustPointUseCase, GrantPointUseCase, CorrectPointLedgerUseCase,
        ReconcilePointBalanceUseCase {

    private final PointLedgerTransactionExecutor transactionExecutor;

    @Override
    public PointLedgerResult adjust(AdjustPointCommand command) {
        boolean joinsCallerTransaction = TransactionSynchronizationManager.isActualTransactionActive();
        String idempotencyKey = idempotencyKey(command.idempotencyKey());
        WriteRequest request = new WriteRequest(
                command.userId(),
                command.displayName(),
                command.delta(),
                command.sourceType(),
                command.sourceReference(),
                description(command),
                normalizePrivateNote(command.privateNote()),
                command.correctionOfLedgerId(),
                normalizeActorUserId(command.actorUserId()),
                idempotencyKey,
                command.allowNegativeBalance(),
                command.createIfMissing()
        );
        try {
            return transactionExecutor.execute(request);
        } catch (DataIntegrityViolationException failure) {
            if (joinsCallerTransaction) {
                // The aggregate transaction is rollback-only; its outer boundary must observe the failure.
                throw failure;
            }
            return transactionExecutor.resolveDuplicate(request).orElseThrow(() -> failure);
        }
    }

    @Override
    public PointLedgerResult grant(AdjustPointCommand command) {
        return adjust(command);
    }

    @Override
    public PointLedgerResult correct(AdjustPointCommand command) {
        return adjust(AdjustPointCommand.builder()
                .userId(command.userId())
                .displayName(command.displayName())
                .delta(command.delta())
                .sourceType(PointSourceType.CORRECTION)
                .sourceReference(command.sourceReference())
                .description(command.description())
                .privateNote(command.privateNote())
                .correctionOfLedgerId(command.correctionOfLedgerId())
                .actorUserId(command.actorUserId())
                .idempotencyKey(command.idempotencyKey())
                .allowNegativeBalance(true)
                .createIfMissing(false)
                .build());
    }

    @Override
    public PointLedgerResult reconcileToBalance(ReconcilePointBalanceCommand command) {
        return transactionExecutor.reconcile(new ReconcileRequest(
                command.userId(),
                command.displayName(),
                command.targetBalance(),
                command.sourceReference(),
                normalizeDescription(command.description(), "데이터 동기화"),
                normalizePrivateNote(command.privateNote()),
                normalizeActorUserId(command.actorUserId()),
                idempotencyKey(null),
                command.createIfMissing()
        ));
    }

    private String idempotencyKey(String value) {
        return value == null || value.isBlank() ? "point:" + UUID.randomUUID() : value;
    }

    private String description(AdjustPointCommand command) {
        String defaultDescription = switch (command.sourceType()) {
            case ADMIN_ADJUSTMENT -> "관리자 조정";
            case PRESENCE_REWARD -> "생존 확인 보상";
            case SHEET_MIGRATION -> "데이터 동기화";
            case REWARD_MANUAL -> "수동 보상";
            case REWARD_ROULETTE -> "룰렛 보상";
            case CORRECTION -> "포인트 정정";
        };
        return normalizeDescription(command.description(), defaultDescription);
    }

    private String normalizeDescription(String value, String defaultDescription) {
        return value == null || value.isBlank() ? defaultDescription : value.trim();
    }

    private String normalizePrivateNote(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeActorUserId(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() || "system".equals(normalized) ? null : normalized;
    }
}
