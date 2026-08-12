package org.nowstart.nyangnyangbot.application.port.in.overlay;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public interface QueueOverlayDisplayUseCase {

    /** 완료된 룰렛 실행의 오버레이 표시 작업을 등록한다. */
    void enqueueRouletteRun(
            @NotNull(message = "rouletteRunId is required")
            @Positive(message = "rouletteRunId must be positive") Long rouletteRunId
    );
}
