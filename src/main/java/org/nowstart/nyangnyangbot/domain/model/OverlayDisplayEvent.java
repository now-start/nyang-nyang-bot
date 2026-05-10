package org.nowstart.nyangnyangbot.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public record OverlayDisplayEvent(
        Long id,
        Long rouletteEventId,
        String nickName,
        Integer roundCount,
        LocalDateTime expiresAt,
        List<RouletteRound> rounds
) {
}
