package org.nowstart.nyangnyangbot.adapter.in.web.overlay.response;

import java.time.format.DateTimeFormatter;
import java.util.List;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.response.RouletteRoundResponse;
import org.nowstart.nyangnyangbot.application.port.in.overlay.ManageOverlayDisplayUseCase.OverlayDisplayResult;

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

    public static OverlayEventResponse from(OverlayDisplayResult result) {
        return new OverlayEventResponse(
                result.displayEventId(),
                result.rouletteEventId(),
                result.nickName(),
                result.roundCount(),
                result.maxAnimatedRounds(),
                result.expiresAt() == null ? null : result.expiresAt().format(DATE_FORMATTER),
                result.rounds().stream().map(RouletteRoundResponse::from).toList()
        );
    }
}
