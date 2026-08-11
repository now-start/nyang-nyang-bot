package org.nowstart.nyangnyangbot.application.port.in.reward;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

public interface GrantRouletteRewardUseCase {

    void grantRoulette(
            @Valid @NotNull(message = "command is required") RouletteRewardCommand command
    );

    record RouletteRewardCommand(
            @NotNull(message = "roundId is required")
            @Positive(message = "roundId must be positive") Long roundId,
            @NotBlank(message = "userId is required") String userId,
            String donorDisplayName,
            @NotBlank(message = "ingestionKey is required") String ingestionKey,
            @NotBlank(message = "label is required") String label,
            @NotNull(message = "rewardType is required") RewardType rewardType,
            @NotNull(message = "conversionMode is required") ConversionMode conversionMode,
            Long pointDelta,
            String description,
            String privateNote
    ) {
    }
}
