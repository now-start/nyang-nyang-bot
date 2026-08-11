package org.nowstart.nyangnyangbot.application.port.in.overlay;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public interface QueueOverlayDisplayUseCase {

    void enqueueRouletteRun(
            @NotNull(message = "rouletteRunId is required")
            @Positive(message = "rouletteRunId must be positive") Long rouletteRunId
    );
}
