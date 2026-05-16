package org.nowstart.nyangnyangbot.adapter.in.web.roulette.response;

import java.util.List;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteTableResult;

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

    public static RouletteTableResponse from(RouletteTableResult result) {
        return new RouletteTableResponse(
                result.id(),
                result.title(),
                result.command(),
                result.pricePerRound(),
                result.active(),
                result.version(),
                result.highRoundThreshold(),
                RouletteValidationResponse.from(result.validation()),
                result.items().stream().map(RouletteItemResponse::from).toList()
        );
    }
}
