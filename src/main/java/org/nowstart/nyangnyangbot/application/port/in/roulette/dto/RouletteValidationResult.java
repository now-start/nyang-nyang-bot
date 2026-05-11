package org.nowstart.nyangnyangbot.application.port.in.roulette.dto;

import java.util.List;

public record RouletteValidationResult(
        Boolean activatable,
        List<String> reasons,
        Integer probabilityTotal,
        Boolean hasLosingItem
) {
}
