package org.nowstart.nyangnyangbot.data.dto.roulette;

import java.time.format.DateTimeFormatter;
import java.util.List;
import org.nowstart.nyangnyangbot.application.model.RouletteEvent;
import org.nowstart.nyangnyangbot.application.model.RouletteRound;
import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.RewardType;
import org.nowstart.nyangnyangbot.data.type.RouletteEventStatus;
import org.nowstart.nyangnyangbot.data.type.RouletteRoundStatus;

public class RouletteRunDto {

    public record Response(
            String status,
            Long eventId,
            String reason,
            Integer roundCount,
            List<RoundResponse> rounds
    ) {

        public static Response ignored(String reason) {
            return new Response("IGNORED", null, reason, 0, List.of());
        }

        public static Response duplicate() {
            return new Response("DUPLICATE", null, "duplicate donation event", 0, List.of());
        }

        public static Response confirmed(RouletteEvent event, List<RouletteRound> rounds) {
            return new Response(
                    "CONFIRMED",
                    event.id(),
                    null,
                    event.roundCount(),
                    rounds.stream().map(RoundResponse::from).toList()
            );
        }
    }

    public record EventResponse(
            Long eventId,
            String donationEventId,
            String userId,
            String nickNameSnapshot,
            Long donationAmount,
            Integer roundCount,
            RouletteEventStatus status,
            String createdAt,
            List<RoundResponse> rounds
    ) {

        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        public static EventResponse from(RouletteEvent event, List<RouletteRound> rounds) {
            String createdAt = event.createdAt() == null ? null : event.createdAt().format(DATE_FORMATTER);
            return new EventResponse(
                    event.id(),
                    event.donationEventId(),
                    event.userId(),
                    event.nickNameSnapshot(),
                    event.donationAmount(),
                    event.roundCount(),
                    event.status(),
                    createdAt,
                    rounds.stream().map(RoundResponse::from).toList()
            );
        }
    }

    public record RoundResponse(
            Long id,
            Integer roundNo,
            String itemLabel,
            Boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            RouletteRoundStatus status,
            Long ledgerId,
            Long userUpboId,
            String failureReason
    ) {

        public static RoundResponse from(RouletteRound round) {
            return new RoundResponse(
                    round.id(),
                    round.roundNo(),
                    round.itemLabel(),
                    round.losingItem(),
                    round.rewardType(),
                    round.conversionMode(),
                    round.exchangeFavoriteValue(),
                    round.status(),
                    round.ledgerId(),
                    round.userUpboId(),
                    round.failureReason()
            );
        }
    }
}
