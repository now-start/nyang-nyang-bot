package org.nowstart.nyangnyangbot.adapter.in.web.roulette.request;

import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

public record RouletteItemRequest(
        String label,
        Integer probabilityBasisPoints,
        Boolean losingItem,
        RewardType rewardType,
        ConversionMode conversionMode,
        Integer exchangeFavoriteValue,
        Integer displayOrder
) {
}
