package org.nowstart.nyangnyangbot.adapter.in.web.roulette.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RouletteTableCreateRequest(
        @NotBlank(message = "title is required")
        String title,
        @NotBlank(message = "command is required")
        String command,
        @NotNull(message = "pricePerRound is required")
        @Positive(message = "pricePerRound is required")
        Long pricePerRound,
        @Positive(message = "highRoundThreshold must be positive")
        Integer highRoundThreshold
) {
}
