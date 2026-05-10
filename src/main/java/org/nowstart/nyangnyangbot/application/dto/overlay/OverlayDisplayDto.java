package org.nowstart.nyangnyangbot.application.dto.overlay;

import java.time.format.DateTimeFormatter;
import java.util.List;
import org.nowstart.nyangnyangbot.domain.model.OverlayDisplayEvent;
import org.nowstart.nyangnyangbot.domain.model.RouletteRound;
import org.nowstart.nyangnyangbot.application.dto.roulette.RouletteRunDto;

public class OverlayDisplayDto {

    public record EventResponse(
            Long displayEventId,
            Long rouletteEventId,
            String nickName,
            Integer roundCount,
            Integer maxAnimatedRounds,
            String expiresAt,
            List<RouletteRunDto.RoundResponse> rounds
    ) {

        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        public static EventResponse from(
                OverlayDisplayEvent displayEvent,
                List<RouletteRound> rounds,
                int maxAnimatedRounds
        ) {
            return new EventResponse(
                    displayEvent.id(),
                    displayEvent.rouletteEventId(),
                    displayEvent.nickName(),
                    displayEvent.roundCount(),
                    maxAnimatedRounds,
                    displayEvent.expiresAt() == null ? null : displayEvent.expiresAt().format(DATE_FORMATTER),
                    rounds.stream().map(RouletteRunDto.RoundResponse::from).toList()
            );
        }
    }
}
