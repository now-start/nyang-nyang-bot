package org.nowstart.nyangnyangbot.application.port.in.reward;

import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

public interface GrantRouletteRewardUseCase {

    void grantRoulette(RouletteRewardCommand command);

    record RouletteRewardCommand(
            Long roundId,
            String userId,
            String donorDisplayName,
            String ingestionKey,
            String label,
            RewardType rewardType,
            ConversionMode conversionMode,
            Long pointDelta,
            String description,
            String privateNote
    ) {
    }
}
