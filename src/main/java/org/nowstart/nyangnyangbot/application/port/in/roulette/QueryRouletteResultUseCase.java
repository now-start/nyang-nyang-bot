package org.nowstart.nyangnyangbot.application.port.in.roulette;

import java.time.LocalDateTime;
import java.util.List;

public interface QueryRouletteResultUseCase {

    List<RouletteRoundResult> getRecentRounds(String userId, int limit);

    List<RouletteEventResult> getUserEvents(String userId);

    record RouletteRoundResult(
            Long id,
            Integer roundNo,
            String itemLabel,
            Boolean losingItem,
            String rewardType,
            String conversionMode,
            Integer exchangeFavoriteValue,
            String status,
            Long ledgerId,
            Long userUpboId,
            String failureReason
    ) {
    }

    record RouletteEventResult(
            Long eventId,
            String donationEventId,
            String userId,
            String nickNameSnapshot,
            Long donationAmount,
            Integer roundCount,
            String status,
            LocalDateTime createdAt,
            List<RouletteRoundResult> rounds
    ) {
    }
}
