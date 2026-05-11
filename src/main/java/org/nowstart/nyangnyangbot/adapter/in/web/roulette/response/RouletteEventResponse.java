package org.nowstart.nyangnyangbot.adapter.in.web.roulette.response;

import java.time.format.DateTimeFormatter;
import java.util.List;
import org.nowstart.nyangnyangbot.application.port.in.roulette.dto.RouletteEventDetail;
import org.nowstart.nyangnyangbot.domain.model.RouletteEvent;
import org.nowstart.nyangnyangbot.domain.type.RouletteEventStatus;

public record RouletteEventResponse(
        Long eventId,
        String donationEventId,
        String userId,
        String nickNameSnapshot,
        Long donationAmount,
        Integer roundCount,
        RouletteEventStatus status,
        String createdAt,
        List<RouletteRoundResponse> rounds
) {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static RouletteEventResponse from(RouletteEventDetail detail) {
        RouletteEvent event = detail.event();
        String createdAt = event.createdAt() == null ? null : event.createdAt().format(DATE_FORMATTER);
        return new RouletteEventResponse(
                event.id(),
                event.donationEventId(),
                event.userId(),
                event.nickNameSnapshot(),
                event.donationAmount(),
                event.roundCount(),
                event.status(),
                createdAt,
                detail.rounds().stream().map(RouletteRoundResponse::from).toList()
        );
    }
}
