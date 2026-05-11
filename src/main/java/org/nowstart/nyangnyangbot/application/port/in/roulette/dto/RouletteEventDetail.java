package org.nowstart.nyangnyangbot.application.port.in.roulette.dto;

import java.util.List;
import org.nowstart.nyangnyangbot.domain.model.RouletteEvent;
import org.nowstart.nyangnyangbot.domain.model.RouletteRound;

public record RouletteEventDetail(
        RouletteEvent event,
        List<RouletteRound> rounds
) {
}
