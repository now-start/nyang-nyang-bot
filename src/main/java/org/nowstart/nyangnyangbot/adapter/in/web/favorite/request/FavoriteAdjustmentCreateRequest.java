package org.nowstart.nyangnyangbot.adapter.in.web.favorite.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentCreateCommand;

public record FavoriteAdjustmentCreateRequest(
        @NotNull(message = "amount is required")
        Integer amount,
        @NotBlank(message = "label is required")
        String label
) {

    public FavoriteAdjustmentCreateCommand toCreateAdjustmentCommand() {
        return new FavoriteAdjustmentCreateCommand(amount, label);
    }
}
