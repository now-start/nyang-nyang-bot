package org.nowstart.nyangnyangbot.application.service.point;

import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase.PointLedgerResult;
import org.nowstart.nyangnyangbot.application.port.out.point.PointLedgerPort;
import org.nowstart.nyangnyangbot.application.port.out.point.PointLedgerPort.AppendPointEntry;
import org.nowstart.nyangnyangbot.application.port.out.point.PointLedgerPort.LedgerEntryRecord;
import org.nowstart.nyangnyangbot.domain.point.PointSourceType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PointLedgerTransactionExecutor {

    private final PointLedgerPort pointLedgerPort;

    @Transactional
    public PointLedgerResult execute(WriteRequest request) {
        boolean userExists = pointLedgerPort.lockUser(
                request.userId(),
                request.displayName(),
                request.createIfMissing()
        );
        if (!userExists) {
            throw new IllegalArgumentException("Point user not found");
        }
        Optional<LedgerEntryRecord> existing = pointLedgerPort.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return duplicate(request, existing.get());
        }

        long beforeBalance = pointLedgerPort.balance(request.userId());
        validateCorrection(request);
        long afterBalance = Math.addExact(beforeBalance, request.delta());
        if (!request.allowNegativeBalance() && afterBalance < 0) {
            throw new IllegalArgumentException("point balance cannot be negative");
        }
        LedgerEntryRecord saved = pointLedgerPort.append(new AppendPointEntry(
                request.userId(),
                request.delta(),
                request.sourceType(),
                request.sourceReference(),
                request.description(),
                request.privateNote(),
                request.correctionOfEntryId(),
                request.actorUserId(),
                request.idempotencyKey()
        ));
        return new PointLedgerResult(saved.id());
    }

    @Transactional(readOnly = true)
    public Optional<PointLedgerResult> resolveDuplicate(WriteRequest request) {
        return pointLedgerPort.findByIdempotencyKey(request.idempotencyKey())
                .map(existing -> {
                    validateReplay(request, existing);
                    return new PointLedgerResult(existing.id());
                });
    }

    @Transactional
    public PointLedgerResult reconcile(ReconcileRequest request) {
        boolean userExists = pointLedgerPort.lockUser(
                request.userId(),
                request.displayName(),
                request.createIfMissing()
        );
        if (!userExists) {
            throw new IllegalArgumentException("Point user not found");
        }
        long beforeBalance = pointLedgerPort.balance(request.userId());
        long delta = Math.subtractExact(request.targetBalance(), beforeBalance);
        if (delta == 0) {
            return PointLedgerResult.noChange();
        }
        LedgerEntryRecord saved = pointLedgerPort.append(new AppendPointEntry(
                request.userId(),
                delta,
                PointSourceType.GOOGLE_SHEET_SYNC,
                request.sourceReference(),
                request.description(),
                request.privateNote(),
                null,
                request.actorUserId(),
                request.idempotencyKey()
        ));
        return new PointLedgerResult(saved.id());
    }

    private PointLedgerResult duplicate(WriteRequest request, LedgerEntryRecord existing) {
        validateReplay(request, existing);
        return new PointLedgerResult(existing.id());
    }

    private void validateReplay(WriteRequest request, LedgerEntryRecord existing) {
        if (!matches(request, existing)) {
            throw new IllegalArgumentException("point idempotency key conflicts with existing entry");
        }
    }

    private boolean matches(WriteRequest request, LedgerEntryRecord existing) {
        return existing.userId().equals(request.userId())
                && existing.delta() == request.delta()
                && existing.sourceType() == request.sourceType()
                && Objects.equals(existing.sourceReference(), request.sourceReference())
                && existing.description().equals(request.description())
                && Objects.equals(existing.privateNote(), request.privateNote())
                && Objects.equals(existing.correctionOfEntryId(), request.correctionOfEntryId())
                && Objects.equals(existing.actorUserId(), request.actorUserId());
    }

    private void validateCorrection(WriteRequest request) {
        if (request.sourceType() != PointSourceType.CORRECTION) {
            if (request.correctionOfEntryId() != null) {
                throw new IllegalArgumentException("Only CORRECTION entries can reference another entry");
            }
            return;
        }
        if (request.correctionOfEntryId() == null) {
            throw new IllegalArgumentException("correctionOfLedgerId is required");
        }
        LedgerEntryRecord target = pointLedgerPort
                .findCorrectionTargetForUpdate(request.correctionOfEntryId())
                .orElseThrow(() -> new IllegalArgumentException("Correction target not found"));
        if (!target.userId().equals(request.userId())) {
            throw new IllegalArgumentException("Correction target must belong to the same user");
        }
        if (pointLedgerPort.hasCorrection(target.id())) {
            throw new IllegalArgumentException("Point ledger entry was already corrected");
        }
    }

    record WriteRequest(
            String userId,
            String displayName,
            long delta,
            PointSourceType sourceType,
            String sourceReference,
            String description,
            String privateNote,
            Long correctionOfEntryId,
            String actorUserId,
            String idempotencyKey,
            boolean allowNegativeBalance,
            boolean createIfMissing
    ) {
    }

    record ReconcileRequest(
            String userId,
            String displayName,
            long targetBalance,
            String sourceReference,
            String description,
            String privateNote,
            String actorUserId,
            String idempotencyKey,
            boolean createIfMissing
    ) {
    }
}
