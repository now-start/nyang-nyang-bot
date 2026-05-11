package org.nowstart.nyangnyangbot.application.port.in.roulette.dto;

import java.util.List;

public record RouletteSimulationResult(
        Integer iterations,
        List<Entry> items
) {

    public record Entry(
            String label,
            Integer count,
            Double ratio
    ) {
    }
}
