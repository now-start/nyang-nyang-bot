package org.nowstart.nyangnyangbot.adapter.in.web.favorite.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentApplyCommand;

public record FavoriteAdjustmentApplyRequest(
        @NotBlank(message = "userId is required")
        String userId,
        List<@Positive(message = "adjustmentIds must be positive") Long> adjustmentIds,
        Integer manualAmount,
        String manualHistory
) {

    public FavoriteAdjustmentApplyCommand toApplyAdjustmentCommand() {
        return new FavoriteAdjustmentApplyCommand(userId, adjustmentIds, manualAmount, manualHistory);
    }
}
