package org.nowstart.nyangnyangbot.adapter.in.web.roulette.response;

import java.util.List;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ProcessRouletteDonationUseCase.RouletteRunResult;

public record RouletteRunResponse(
        String status,
        Long eventId,
        String reason,
        Integer roundCount,
        List<RouletteRoundResponse> rounds
) {

    public static RouletteRunResponse from(RouletteRunResult result) {
        return new RouletteRunResponse(
                result.status().name(),
                result.eventId(),
                result.reason(),
                result.roundCount(),
                result.rounds().stream().map(RouletteRoundResponse::from).toList()
        );
    }
}
