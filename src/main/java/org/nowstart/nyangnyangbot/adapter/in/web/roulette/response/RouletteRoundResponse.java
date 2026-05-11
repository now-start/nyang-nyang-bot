package org.nowstart.nyangnyangbot.adapter.in.web.roulette.response;

import org.nowstart.nyangnyangbot.domain.model.RouletteRound;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;

public record RouletteRoundResponse(
        Long id,
        Integer roundNo,
        String itemLabel,
        Boolean losingItem,
        RewardType rewardType,
        ConversionMode conversionMode,
        Integer exchangeFavoriteValue,
        RouletteRoundStatus status,
        Long ledgerId,
        Long userUpboId,
        String failureReason
) {

    public static RouletteRoundResponse from(RouletteRound round) {
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
