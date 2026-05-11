package org.nowstart.nyangnyangbot.application.port.out.roulette.dto;

import org.nowstart.nyangnyangbot.domain.type.RouletteEventStatus;

public record CreateRouletteEventCommand(
        String donationEventId,
        String idempotencyKey,
        String userId,
        String nickNameSnapshot,
        Long donationAmount,
        String donationText,
        Long rouletteTableId,
        Integer rouletteTableVersion,
        String command,
        Long pricePerRound,
        Integer roundCount,
        String itemsSnapshotJson,
        RouletteEventStatus status
) {
}
