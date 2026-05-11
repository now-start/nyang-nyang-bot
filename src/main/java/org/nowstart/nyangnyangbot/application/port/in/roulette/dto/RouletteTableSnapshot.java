package org.nowstart.nyangnyangbot.application.port.in.roulette.dto;

import java.util.List;
import org.nowstart.nyangnyangbot.domain.model.RouletteItem;
import org.nowstart.nyangnyangbot.domain.model.RouletteTable;

public record RouletteTableSnapshot(
        RouletteTable table,
        List<RouletteItem> items,
        RouletteValidationResult validation
) {
}
