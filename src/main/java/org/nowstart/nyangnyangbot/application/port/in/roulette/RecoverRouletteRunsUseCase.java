package org.nowstart.nyangnyangbot.application.port.in.roulette;

public interface RecoverRouletteRunsUseCase {

    int DEFAULT_BATCH_SIZE = 100;
    int MIN_BATCH_SIZE = 1;
    int MAX_BATCH_SIZE = 100;

    void recoverRun(Long runId);

    int recoverPendingRuns(int limit);
}
