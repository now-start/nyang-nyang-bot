package org.nowstart.nyangnyangbot.application.port.out.overlay;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;

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
            List<RoundResult> rounds
    ) {
    }
}
