package org.nowstart.nyangnyangbot.adapter.in.web.favorite.response;

import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentOptionResult;

public record FavoriteAdjustmentOptionResponse(
        Long id,
        Integer amount,
        String label
) {

    public static FavoriteAdjustmentOptionResponse from(FavoriteAdjustmentOptionResult result) {
        return new FavoriteAdjustmentOptionResponse(
                result.id(),
                result.amount(),
                result.label()
        );
    }
}
