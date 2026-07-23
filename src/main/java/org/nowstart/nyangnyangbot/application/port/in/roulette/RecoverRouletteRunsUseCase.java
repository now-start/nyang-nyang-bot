package org.nowstart.nyangnyangbot.application.port.in.roulette;

public interface RecoverRouletteRunsUseCase {

    void recoverRun(Long runId);

    int recoverPendingRuns(int limit);
}
