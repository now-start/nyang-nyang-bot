package org.nowstart.nyangnyangbot.application.port.out.point;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
            @NotBlank(message = "userId is required") String userId,
            long delta,
            @NotNull(message = "sourceType is required") PointSourceType sourceType,
            String sourceReference,
            @NotBlank(message = "description is required") String description,
            String privateNote,
            Long correctionOfEntryId,
            String actorUserId,
            @NotBlank(message = "idempotencyKey is required") String idempotencyKey
    ) {
        @AssertTrue(message = "delta must not be zero")
        public boolean isDeltaNonZero() {
            return delta != 0;
        }
    }

    record LedgerEntryRecord(
            @Positive(message = "id must be positive") long id,
            @NotBlank(message = "userId is required") String userId,
            long delta,
            @NotNull(message = "sourceType is required") PointSourceType sourceType,
            String sourceReference,
            @NotBlank(message = "description is required") String description,
            String privateNote,
            Long correctionOfEntryId,
            String actorUserId
    ) {
        @AssertTrue(message = "delta must not be zero")
        public boolean isDeltaNonZero() {
            return delta != 0;
        }
    }
}
