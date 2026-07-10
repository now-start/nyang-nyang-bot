package org.nowstart.nyangnyangbot.application.port.out.overlay;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;

public interface OverlayDisplayPort {

    void enqueueRouletteEvent(Long rouletteEventId, LocalDateTime expiresAt);

    DisplayEventResult replayRouletteEvent(Long rouletteEventId, LocalDateTime expiresAt);

    void markPendingExpiredBefore(LocalDateTime current);

    Optional<DisplayEventResult> claimNextPending(LocalDateTime current);

    void markDisplayed(Long displayEventId, LocalDateTime displayedAt);

    record DisplayEventResult(
            Long id,
            Long rouletteEventId,
            String nickName,
            Integer roundCount,
            LocalDateTime expiresAt,
            List<DisplayRoundResult> rounds
    ) {
    }

    record DisplayRoundResult(
            Long id,
            Integer roundNo,
            String itemLabel,
            boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            RouletteRoundStatus status,
            Long ledgerId,
            Long userUpboId,
            String failureReason
    ) {
    }
}
