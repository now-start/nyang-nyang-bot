package org.nowstart.nyangnyangbot.adapter.in.web.roulette.response;

import java.time.format.DateTimeFormatter;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteEventSummaryResult;

public record RouletteEventSummaryResponse(
        Long eventId,
        String donationEventId,
        String userId,
        String nickNameSnapshot,
        Long donationAmount,
        Integer roundCount,
        String status,
        String createdAt
) {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static RouletteEventSummaryResponse from(RouletteEventSummaryResult result) {
        String createdAt = result.createdAt() == null ? null : result.createdAt().format(DATE_FORMATTER);
        return new RouletteEventSummaryResponse(
                result.eventId(),
                result.donationEventId(),
                result.userId(),
                result.nickNameSnapshot(),
                result.donationAmount(),
                result.roundCount(),
                result.status(),
                createdAt
        );
    }
}
