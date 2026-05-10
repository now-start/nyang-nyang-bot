package org.nowstart.nyangnyangbot.application.model;

import java.time.LocalDateTime;
import org.nowstart.nyangnyangbot.domain.type.RouletteEventStatus;

public record RouletteEvent(
        Long id,
        String donationEventId,
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
        RouletteEventStatus status,
        LocalDateTime createdAt
) {
}
