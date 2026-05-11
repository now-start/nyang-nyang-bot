package org.nowstart.nyangnyangbot.adapter.in.web.favorite.request;

import java.util.List;
import org.nowstart.nyangnyangbot.application.port.in.favorite.dto.FavoriteAdjustmentApplyCommand;

public record FavoriteAdjustmentApplyRequest(
        String userId,
        List<Long> adjustmentIds,
        Integer manualAmount,
        String manualHistory
) {

    public FavoriteAdjustmentApplyCommand toCommand() {
        return new FavoriteAdjustmentApplyCommand(userId, adjustmentIds, manualAmount, manualHistory);
    }
}
