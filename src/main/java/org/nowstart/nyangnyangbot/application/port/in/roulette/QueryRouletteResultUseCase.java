package org.nowstart.nyangnyangbot.application.port.in.roulette;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QueryRouletteResultUseCase {

    List<RouletteRoundResult> getRecentRounds(String userId, int limit);

    List<RouletteEventResult> getUserEvents(String userId);

    Page<RouletteEventSummaryResult> getRecentEvents(Pageable pageable);

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

    record RouletteEventSummaryResult(
            Long eventId,
            String donationEventId,
            String userId,
            String nickNameSnapshot,
            Long donationAmount,
            Integer roundCount,
            String status,
            LocalDateTime createdAt
    ) {
    }
}
