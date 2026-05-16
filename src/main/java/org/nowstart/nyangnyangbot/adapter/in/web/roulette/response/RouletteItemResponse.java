package org.nowstart.nyangnyangbot.adapter.in.web.roulette.response;

import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteItemResult;

public record RouletteItemResponse(
        Long id,
        String label,
        Integer probabilityBasisPoints,
        Boolean losingItem,
        String rewardType,
        String conversionMode,
        Integer exchangeFavoriteValue,
        Boolean active,
        Integer displayOrder
) {

    public static RouletteItemResponse from(RouletteItemResult item) {
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
