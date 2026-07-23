package org.nowstart.nyangnyangbot.application.port.in.point;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.nowstart.nyangnyangbot.domain.point.PointSourceType;

public interface AdjustPointUseCase {

    PointLedgerResult adjust(@Valid @NotNull(message = "command is required") AdjustPointCommand command);

    @Builder
    record AdjustPointCommand(
            @NotBlank(message = "userId is required") String userId,
            String displayName,
            long delta,
            @NotNull(message = "sourceType is required") PointSourceType sourceType,
            String sourceReference,
            String description,
            String privateNote,
            Long correctionOfLedgerId,
            String actorUserId,
            String idempotencyKey,
            boolean allowNegativeBalance,
            boolean createIfMissing
    ) {
        @AssertTrue(message = "delta must not be zero")
        public boolean isDeltaNonZero() {
            return delta != 0;
        }
    }

    record PointLedgerResult(
            String userId,
            long beforeBalance,
            long delta,
            long afterBalance,
            String description,
            boolean duplicate,
            Long ledgerId
    ) {
        public static PointLedgerResult duplicate(String userId, long balance, Long ledgerId) {
            return new PointLedgerResult(userId, balance, 0, balance, null, true, ledgerId);
        }
    }
}
