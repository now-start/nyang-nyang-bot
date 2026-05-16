package org.nowstart.nyangnyangbot.adapter.in.web.favorite.request;

import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentCreateCommand;

public record FavoriteAdjustmentCreateRequest(
        Integer amount,
        String label
) {

    public FavoriteAdjustmentCreateCommand toCreateAdjustmentCommand() {
        return new FavoriteAdjustmentCreateCommand(amount, label);
    }
}
