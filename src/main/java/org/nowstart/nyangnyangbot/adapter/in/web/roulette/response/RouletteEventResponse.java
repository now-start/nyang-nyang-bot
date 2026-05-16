package org.nowstart.nyangnyangbot.adapter.in.web.roulette.response;

import java.time.format.DateTimeFormatter;
import java.util.List;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteEventResult;

public record RouletteEventResponse(
        Long eventId,
        String donationEventId,
        String userId,
        String nickNameSnapshot,
        Long donationAmount,
        Integer roundCount,
        String status,
        String createdAt,
        List<RouletteRoundResponse> rounds
) {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static RouletteEventResponse from(RouletteEventResult result) {
        String createdAt = result.createdAt() == null ? null : result.createdAt().format(DATE_FORMATTER);
        return new RouletteEventResponse(
                result.eventId(),
                result.donationEventId(),
                result.userId(),
                result.nickNameSnapshot(),
                result.donationAmount(),
                result.roundCount(),
                result.status(),
                createdAt,
                result.rounds().stream().map(RouletteRoundResponse::from).toList()
        );
    }
}
