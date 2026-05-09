package org.nowstart.nyangnyangbot.data.dto.roulette;

import java.time.format.DateTimeFormatter;
import java.util.List;
import org.nowstart.nyangnyangbot.data.entity.RouletteEventEntity;
import org.nowstart.nyangnyangbot.data.entity.RouletteRoundResultEntity;
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

        public static Response confirmed(RouletteEventEntity event, List<RouletteRoundResultEntity> rounds) {
            return new Response(
                    "CONFIRMED",
                    event.getId(),
                    null,
                    event.getRoundCount(),
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

        public static EventResponse from(RouletteEventEntity event, List<RouletteRoundResultEntity> rounds) {
            String createdAt = event.getCreateDate() == null ? null : event.getCreateDate().format(DATE_FORMATTER);
            return new EventResponse(
                    event.getId(),
                    event.getDonationEventId(),
                    event.getUserId(),
                    event.getNickNameSnapshot(),
                    event.getDonationAmount(),
                    event.getRoundCount(),
                    event.getStatus(),
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

        public static RoundResponse from(RouletteRoundResultEntity round) {
            return new RoundResponse(
                    round.getId(),
                    round.getRoundNo(),
                    round.getItemLabel(),
                    round.isLosingItem(),
                    round.getRewardType(),
                    round.getConversionMode(),
                    round.getExchangeFavoriteValue(),
                    round.getStatus(),
                    round.getLedgerId(),
                    round.getUserUpboId(),
                    round.getFailureReason()
            );
        }
    }
}
