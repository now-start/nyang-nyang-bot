package org.nowstart.nyangnyangbot.application.port.in.overlay;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteRoundResult;

public interface ManageOverlayDisplayUseCase {

    OverlayDisplayResult replayRouletteRun(Long rouletteRunId);

    Optional<OverlayDisplayResult> claimNextJob(String authorizationHeader);

    void markDisplayed(Long displayJobId, String claimToken, String authorizationHeader);

    record OverlayDisplayResult(
            Long displayJobId,
            Long rouletteRunId,
            String donorDisplayName,
            String claimToken,
            Integer roundCount,
            Integer maxAnimatedRounds,
            Instant expiresAt,
            List<RouletteRoundResult> rounds
    ) {
    }
}
