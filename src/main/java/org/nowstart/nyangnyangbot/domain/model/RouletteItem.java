package org.nowstart.nyangnyangbot.domain.model;

import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

public record RouletteItem(
        Long id,
        Long tableId,
        String label,
        Integer probabilityBasisPoints,
        boolean losingItem,
        RewardType rewardType,
        ConversionMode conversionMode,
        Integer exchangeFavoriteValue,
        boolean active,
        Integer displayOrder
) {
}
