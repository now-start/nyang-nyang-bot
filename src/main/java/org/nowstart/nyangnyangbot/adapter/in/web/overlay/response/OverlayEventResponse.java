package org.nowstart.nyangnyangbot.adapter.in.web.overlay.response;

import java.time.format.DateTimeFormatter;
import java.util.List;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.response.RouletteRoundResponse;
import org.nowstart.nyangnyangbot.application.port.in.overlay.dto.OverlayDisplayDetail;
import org.nowstart.nyangnyangbot.domain.model.OverlayDisplayEvent;

public record OverlayEventResponse(
        Long displayEventId,
        Long rouletteEventId,
        String nickName,
        Integer roundCount,
        Integer maxAnimatedRounds,
        String expiresAt,
        List<RouletteRoundResponse> rounds
) {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static OverlayEventResponse from(OverlayDisplayDetail detail) {
        OverlayDisplayEvent displayEvent = detail.displayEvent();
        return new OverlayEventResponse(
                displayEvent.id(),
                displayEvent.rouletteEventId(),
                displayEvent.nickName(),
                displayEvent.roundCount(),
                detail.maxAnimatedRounds(),
                displayEvent.expiresAt() == null ? null : displayEvent.expiresAt().format(DATE_FORMATTER),
                detail.rounds().stream().map(RouletteRoundResponse::from).toList()
        );
    }
}
