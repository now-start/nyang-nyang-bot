package org.nowstart.nyangnyangbot.data.dto.favorite;

import java.util.List;

public class FavoriteAdjustmentDto {

    public record ApplyRequest(
            String userId,
            List<Long> adjustmentIds,
            Integer manualAmount,
            String manualHistory
    ) {
    }

    public record ApplyResponse(
            String userId,
            Integer beforeFavorite,
            Integer delta,
            Integer afterFavorite,
            String history
    ) {
    }

    public record CreateRequest(Integer amount, String label) {
    }
}
