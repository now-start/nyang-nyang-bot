package org.nowstart.nyangnyangbot.domain.roulette;

import java.util.List;

public record RouletteActivationValidation(
        boolean activatable,
        List<String> reasons,
        int probabilityTotal,
        boolean hasLosingOption
) {
}
