package org.nowstart.nyangnyangbot.application.port.in.overlay;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteRoundResult;

public interface ManageOverlayDisplayUseCase {

    void replayRouletteRun(
            @NotNull(message = "rouletteRunId is required")
            @Positive(message = "rouletteRunId must be positive") Long rouletteRunId
    );

    Optional<OverlayDisplayResult> claimNextJob(
            @NotBlank(message = "authorizationHeader is required") String authorizationHeader
    );

    void markDisplayed(
            @NotNull(message = "displayJobId is required")
            @Positive(message = "displayJobId must be positive") Long displayJobId,
            @NotBlank(message = "claimToken is required") String claimToken,
            @NotBlank(message = "authorizationHeader is required") String authorizationHeader
    );

    record OverlayDisplayResult(
            Long displayJobId,
            String donorDisplayName,
            String claimToken,
            Integer roundCount,
            List<RouletteRoundResult> rounds
    ) {
    }
}
