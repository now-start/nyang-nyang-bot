package org.nowstart.nyangnyangbot.application.port.in.favorite.dto;

public record FavoriteAdjustmentCreateCommand(
        Integer amount,
        String label
) {
}
