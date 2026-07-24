package org.nowstart.nyangnyangbot.application.port.in.overlay;

import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteRoundResult;

public interface ManageOverlayDisplayUseCase {

    void replayRouletteRun(Long rouletteRunId);

    Optional<OverlayDisplayResult> claimNextJob(String authorizationHeader);

    void markDisplayed(Long displayJobId, String claimToken, String authorizationHeader);

    record OverlayDisplayResult(
            Long displayJobId,
            String donorDisplayName,
            String claimToken,
            Integer roundCount,
            List<RouletteRoundResult> rounds
    ) {
    }
}
