package org.nowstart.nyangnyangbot.adapter.in.web.roulette.response;

import org.nowstart.nyangnyangbot.domain.model.RouletteItem;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

public record RouletteItemResponse(
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

    public static RouletteItemResponse from(RouletteItem item) {
        return new RouletteItemResponse(
                item.id(),
                item.label(),
                item.probabilityBasisPoints(),
                item.losingItem(),
                item.rewardType(),
                item.conversionMode(),
                item.exchangeFavoriteValue(),
                item.active(),
                item.displayOrder()
        );
    }
}
