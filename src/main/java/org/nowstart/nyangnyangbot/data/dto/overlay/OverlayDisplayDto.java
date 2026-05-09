package org.nowstart.nyangnyangbot.data.dto.overlay;

import java.time.format.DateTimeFormatter;
import java.util.List;
import org.nowstart.nyangnyangbot.data.dto.roulette.RouletteRunDto;
import org.nowstart.nyangnyangbot.data.entity.OverlayDisplayEventEntity;
import org.nowstart.nyangnyangbot.data.entity.RouletteRoundResultEntity;

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
                OverlayDisplayEventEntity displayEvent,
                List<RouletteRoundResultEntity> rounds,
                int maxAnimatedRounds
        ) {
            return new EventResponse(
                    displayEvent.getId(),
                    displayEvent.getRouletteEvent().getId(),
                    displayEvent.getRouletteEvent().getNickNameSnapshot(),
                    displayEvent.getRouletteEvent().getRoundCount(),
                    maxAnimatedRounds,
                    displayEvent.getExpiresAt() == null ? null : displayEvent.getExpiresAt().format(DATE_FORMATTER),
                    rounds.stream().map(RouletteRunDto.RoundResponse::from).toList()
            );
        }
    }
}
