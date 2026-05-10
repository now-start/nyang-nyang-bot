package org.nowstart.nyangnyangbot.application.port.out.roulette;

import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.RewardType;
import org.nowstart.nyangnyangbot.data.type.RouletteRoundStatus;

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
