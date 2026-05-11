package org.nowstart.nyangnyangbot.application.port.in.favorite.dto;

public record FavoriteAdjustmentApplyResult(
        String userId,
        Integer beforeFavorite,
        Integer delta,
        Integer afterFavorite,
        String history
) {
}
