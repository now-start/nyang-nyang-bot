package org.nowstart.nyangnyangbot.application.port.in.roulette;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public interface RecoverRouletteRunsUseCase {

    int DEFAULT_BATCH_SIZE = 100;
    int MIN_BATCH_SIZE = 1;
    int MAX_BATCH_SIZE = 100;

    /** 지정한 룰렛 실행에서 미적용된 회차를 다시 처리한다. */
    void recoverRun(
            @NotNull(message = "runId is required")
            @Positive(message = "runId must be positive") Long runId
    );

    /** 대기 중인 실행을 최대 {@code limit}개 복구하고 성공한 개수를 반환한다. */
    int recoverPendingRuns(
            @Min(value = MIN_BATCH_SIZE, message = "limit must be at least 1")
            @Max(value = MAX_BATCH_SIZE, message = "limit must be 100 or less") int limit
    );
}
