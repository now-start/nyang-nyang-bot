package org.nowstart.nyangnyangbot.adapter.in.web.roulette.response;

import java.util.List;
import org.nowstart.nyangnyangbot.application.port.in.roulette.dto.RouletteValidationResult;

public record RouletteValidationResponse(
        Boolean activatable,
        List<String> reasons,
        Integer probabilityTotal,
        Boolean hasLosingItem
) {

    public static RouletteValidationResponse from(RouletteValidationResult result) {
        return new RouletteValidationResponse(
                result.activatable(),
                result.reasons(),
                result.probabilityTotal(),
                result.hasLosingItem()
        );
    }
}
