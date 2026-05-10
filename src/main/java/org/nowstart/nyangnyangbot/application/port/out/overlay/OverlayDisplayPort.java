package org.nowstart.nyangnyangbot.application.port.out.overlay;

import java.time.LocalDateTime;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.model.OverlayDisplayEvent;

public interface OverlayDisplayPort {

    void enqueueRouletteEvent(Long rouletteEventId, LocalDateTime expiresAt);

    OverlayDisplayEvent replayRouletteEvent(Long rouletteEventId, LocalDateTime expiresAt);

    void markPendingExpiredBefore(LocalDateTime current);

    Optional<OverlayDisplayEvent> claimNextPending(LocalDateTime current);

    void markDisplayed(Long displayEventId, LocalDateTime displayedAt);
}
