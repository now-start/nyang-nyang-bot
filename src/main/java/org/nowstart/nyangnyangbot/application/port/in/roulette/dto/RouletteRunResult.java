package org.nowstart.nyangnyangbot.application.port.in.roulette.dto;

import java.util.List;
import org.nowstart.nyangnyangbot.domain.model.RouletteEvent;
import org.nowstart.nyangnyangbot.domain.model.RouletteRound;

public record RouletteRunResult(
        Status status,
        RouletteEvent event,
        String reason,
        Integer roundCount,
        List<RouletteRound> rounds
) {

    public enum Status {
        CONFIRMED,
        IGNORED,
        DUPLICATE
    }

    public static RouletteRunResult ignored(String reason) {
        return new RouletteRunResult(Status.IGNORED, null, reason, 0, List.of());
    }

    public static RouletteRunResult duplicate() {
        return new RouletteRunResult(Status.DUPLICATE, null, "duplicate donation event", 0, List.of());
    }

    public static RouletteRunResult confirmed(RouletteEvent event, List<RouletteRound> rounds) {
        return new RouletteRunResult(Status.CONFIRMED, event, null, event.roundCount(), rounds);
    }
}
