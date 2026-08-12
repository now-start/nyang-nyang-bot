package org.nowstart.nyangnyangbot.application.port.in.point;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.nowstart.nyangnyangbot.domain.point.PointSourceType;

public interface AdjustPointUseCase {

    /** 포인트 증감분을 반영하고 생성된 원장 항목을 반환하며, 멱등 중복 요청이면 변경 없음으로 반환한다. */
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

    record PointLedgerResult(Long ledgerId) {
        public static PointLedgerResult noChange() {
            return new PointLedgerResult(null);
        }
    }
}
