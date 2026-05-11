package org.nowstart.nyangnyangbot.adapter.in.web.favorite.request;

import org.nowstart.nyangnyangbot.application.port.in.favorite.dto.FavoriteAdjustmentCreateCommand;

public record FavoriteAdjustmentCreateRequest(
        Integer amount,
        String label
) {

    public FavoriteAdjustmentCreateCommand toCommand() {
        return new FavoriteAdjustmentCreateCommand(amount, label);
    }
}
