package org.nowstart.nyangnyangbot.application.port.out.point;

import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.point.PointSourceType;

public interface PointLedgerPort {

    boolean lockUser(String userId, String displayName, boolean createIfMissing);

    long balance(String userId);

    Optional<LedgerEntryRecord> findByIdempotencyKey(String idempotencyKey);

    Optional<LedgerEntryRecord> findCorrectionTargetForUpdate(long ledgerEntryId);

    boolean hasCorrection(long ledgerEntryId);

    LedgerEntryRecord append(AppendPointEntry data);

    record AppendPointEntry(
            String userId,
            long delta,
            PointSourceType sourceType,
            String sourceReference,
            String description,
            String privateNote,
            Long correctionOfEntryId,
            String actorUserId,
            String idempotencyKey
    ) {
    }

    record LedgerEntryRecord(
            long id,
            String userId,
            long delta,
            PointSourceType sourceType,
            String sourceReference,
            String description,
            String privateNote,
            Long correctionOfEntryId,
            String actorUserId,
            String idempotencyKey
    ) {
    }
}
