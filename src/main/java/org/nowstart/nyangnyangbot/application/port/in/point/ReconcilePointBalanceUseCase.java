package org.nowstart.nyangnyangbot.application.port.in.point;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase.PointLedgerResult;

public interface ReconcilePointBalanceUseCase {

    PointLedgerResult reconcileToBalance(
            @Valid @NotNull(message = "command is required") ReconcilePointBalanceCommand command
    );

    @Builder
    record ReconcilePointBalanceCommand(
            @NotBlank(message = "userId is required") String userId,
            String displayName,
            long targetBalance,
            String sourceReference,
            @Size(max = 500, message = "description length must be 500 or less") String description,
            @Size(max = 500, message = "privateNote length must be 500 or less") String privateNote,
            String actorUserId,
            boolean createIfMissing
    ) {
    }
}
