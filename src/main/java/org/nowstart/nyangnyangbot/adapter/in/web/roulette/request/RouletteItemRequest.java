package org.nowstart.nyangnyangbot.adapter.in.web.roulette.request;

public record RouletteItemRequest(
        String label,
        Integer probabilityBasisPoints,
        Boolean losingItem,
        String rewardType,
        String conversionMode,
        Integer exchangeFavoriteValue,
        Integer displayOrder
) {
}
