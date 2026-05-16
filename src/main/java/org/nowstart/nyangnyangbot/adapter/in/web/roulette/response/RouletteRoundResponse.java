package org.nowstart.nyangnyangbot.adapter.in.web.roulette.response;

import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteRoundResult;

public record RouletteRoundResponse(
        Long id,
        Integer roundNo,
        String itemLabel,
        Boolean losingItem,
        String rewardType,
        String conversionMode,
        Integer exchangeFavoriteValue,
        String status,
        Long ledgerId,
        Long userUpboId,
        String failureReason
) {

    public static RouletteRoundResponse from(RouletteRoundResult round) {
        return new RouletteRoundResponse(
                round.id(),
                round.roundNo(),
                round.itemLabel(),
                round.losingItem(),
                round.rewardType(),
                round.conversionMode(),
                round.exchangeFavoriteValue(),
                round.status(),
                round.ledgerId(),
                round.userUpboId(),
                round.failureReason()
        );
    }
}
