package org.nowstart.nyangnyangbot.application.gateway.out.roulette;

import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;

public record CreateRouletteRoundCommand(
        Integer roundNo,
        String itemLabel,
        Integer probabilityBasisPoints,
        boolean losingItem,
        RewardType rewardType,
        ConversionMode conversionMode,
        Integer exchangeFavoriteValue,
        RouletteRoundStatus status,
        Integer ticket
) {
}
