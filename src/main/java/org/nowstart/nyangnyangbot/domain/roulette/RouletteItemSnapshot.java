package org.nowstart.nyangnyangbot.domain.roulette;

import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

public record RouletteItemSnapshot(
        Long id,
        String label,
        Integer probabilityBasisPoints,
        Boolean losingItem,
        RewardType rewardType,
        ConversionMode conversionMode,
        Integer exchangeFavoriteValue,
        Boolean active,
        Integer displayOrder
) {
}
