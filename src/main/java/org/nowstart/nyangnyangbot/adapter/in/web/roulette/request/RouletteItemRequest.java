package org.nowstart.nyangnyangbot.adapter.in.web.roulette.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RouletteItemRequest(
        @NotBlank(message = "label is required")
        String label,
        @NotNull(message = "probabilityBasisPoints is required")
        @PositiveOrZero(message = "probabilityBasisPoints must be between 0 and 10000")
        @Max(value = 10000, message = "probabilityBasisPoints must be between 0 and 10000")
        Integer probabilityBasisPoints,
        Boolean losingItem,
        @NotBlank(message = "rewardType is required")
        String rewardType,
        @NotBlank(message = "conversionMode is required")
        String conversionMode,
        Integer exchangeFavoriteValue,
        Integer displayOrder
) {
}
