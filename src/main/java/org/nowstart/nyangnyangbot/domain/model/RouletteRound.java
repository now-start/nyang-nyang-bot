package org.nowstart.nyangnyangbot.domain.model;

import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;

public record RouletteRound(
        Long id,
        Long rouletteEventId,
        String rouletteEventDonationEventId,
        String rouletteEventUserId,
        String rouletteEventNickNameSnapshot,
        Integer roundNo,
        String itemLabel,
        Integer probabilityBasisPoints,
        boolean losingItem,
        RewardType rewardType,
        ConversionMode conversionMode,
        Integer exchangeFavoriteValue,
        RouletteRoundStatus status,
        Long ledgerId,
        Long userUpboId,
        String failureReason,
        Integer ticket
) {
}
