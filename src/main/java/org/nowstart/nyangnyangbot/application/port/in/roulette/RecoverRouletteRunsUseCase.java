package org.nowstart.nyangnyangbot.application.port.in.roulette;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public interface RecoverRouletteRunsUseCase {

    int DEFAULT_BATCH_SIZE = 100;
    int MIN_BATCH_SIZE = 1;
    int MAX_BATCH_SIZE = 100;

    void recoverRun(
            @NotNull(message = "runId is required")
            @Positive(message = "runId must be positive") Long runId
    );

    int recoverPendingRuns(
            @Min(value = MIN_BATCH_SIZE, message = "limit must be at least 1")
            @Max(value = MAX_BATCH_SIZE, message = "limit must be 100 or less") int limit
    );
}
