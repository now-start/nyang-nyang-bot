package org.nowstart.nyangnyangbot.application.model;

import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.RewardType;

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
