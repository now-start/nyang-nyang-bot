package org.nowstart.nyangnyangbot.adapter.in.web.roulette.response;

import java.util.List;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteSimulationResult;

public record RouletteSimulationResponse(
        Integer iterations,
        List<SimulationItem> items
) {

    public static RouletteSimulationResponse from(RouletteSimulationResult result) {
        return new RouletteSimulationResponse(
                result.iterations(),
                result.items().stream().map(SimulationItem::from).toList()
        );
    }

    public record SimulationItem(
            String label,
            Integer count,
            Double ratio
    ) {

        public static SimulationItem from(RouletteSimulationResult.Entry entry) {
            return new SimulationItem(entry.label(), entry.count(), entry.ratio());
        }
    }
}
