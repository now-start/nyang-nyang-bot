package org.nowstart.nyangnyangbot.application.port.in.overlay.dto;

import java.util.List;
import org.nowstart.nyangnyangbot.domain.model.OverlayDisplayEvent;
import org.nowstart.nyangnyangbot.domain.model.RouletteRound;

public record OverlayDisplayDetail(
        OverlayDisplayEvent displayEvent,
        List<RouletteRound> rounds,
        Integer maxAnimatedRounds
) {
}
