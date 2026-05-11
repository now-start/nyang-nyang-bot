package org.nowstart.nyangnyangbot.adapter.in.web.roulette.response;

import java.util.List;
import org.nowstart.nyangnyangbot.application.port.in.roulette.dto.RouletteTableSnapshot;
import org.nowstart.nyangnyangbot.domain.model.RouletteTable;

public record RouletteTableResponse(
        Long id,
        String title,
        String command,
        Long pricePerRound,
        Boolean active,
        Integer version,
        Integer highRoundThreshold,
        RouletteValidationResponse validation,
        List<RouletteItemResponse> items
) {

    public static RouletteTableResponse from(RouletteTableSnapshot snapshot) {
        RouletteTable table = snapshot.table();
        return new RouletteTableResponse(
                table.id(),
                table.title(),
                table.command(),
                table.pricePerRound(),
                table.active(),
                table.version(),
                table.highRoundThreshold(),
                RouletteValidationResponse.from(snapshot.validation()),
                snapshot.items().stream().map(RouletteItemResponse::from).toList()
        );
    }
}
