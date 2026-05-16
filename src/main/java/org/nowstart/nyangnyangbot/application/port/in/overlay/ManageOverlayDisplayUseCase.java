package org.nowstart.nyangnyangbot.application.port.in.overlay;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteRoundResult;

public interface ManageOverlayDisplayUseCase {

    OverlayDisplayResult replayRouletteEvent(Long rouletteEventId);

    Optional<OverlayDisplayResult> claimNextEvent(String authorizationHeader);

    void markDisplayed(Long displayEventId, String authorizationHeader);

    record OverlayDisplayResult(
            Long displayEventId,
            Long rouletteEventId,
            String nickName,
            Integer roundCount,
            Integer maxAnimatedRounds,
            LocalDateTime expiresAt,
            List<RouletteRoundResult> rounds
    ) {
    }
}
