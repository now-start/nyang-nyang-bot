package org.nowstart.nyangnyangbot.adapter.in.web.favorite.request;

import java.util.List;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentApplyCommand;

public record FavoriteAdjustmentApplyRequest(
        String userId,
        List<Long> adjustmentIds,
        Integer manualAmount,
        String manualHistory
) {

    public FavoriteAdjustmentApplyCommand toApplyAdjustmentCommand() {
        return new FavoriteAdjustmentApplyCommand(userId, adjustmentIds, manualAmount, manualHistory);
    }
}
