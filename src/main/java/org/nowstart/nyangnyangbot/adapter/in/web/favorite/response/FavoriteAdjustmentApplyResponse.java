package org.nowstart.nyangnyangbot.adapter.in.web.favorite.response;

import org.nowstart.nyangnyangbot.application.port.in.favorite.dto.FavoriteAdjustmentApplyResult;

public record FavoriteAdjustmentApplyResponse(
        String userId,
        Integer beforeFavorite,
        Integer delta,
        Integer afterFavorite,
        String history
) {

    public static FavoriteAdjustmentApplyResponse from(FavoriteAdjustmentApplyResult result) {
        return new FavoriteAdjustmentApplyResponse(
                result.userId(),
                result.beforeFavorite(),
                result.delta(),
                result.afterFavorite(),
                result.history()
        );
    }
}
