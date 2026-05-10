package org.nowstart.nyangnyangbot.application.model;

import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.RewardType;
import org.nowstart.nyangnyangbot.data.type.RouletteRoundStatus;

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
