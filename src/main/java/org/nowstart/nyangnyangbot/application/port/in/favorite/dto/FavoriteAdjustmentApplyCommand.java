package org.nowstart.nyangnyangbot.application.port.in.favorite.dto;

import java.util.List;

public record FavoriteAdjustmentApplyCommand(
        String userId,
        List<Long> adjustmentIds,
        Integer manualAmount,
        String manualHistory
) {
}
