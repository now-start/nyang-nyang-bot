package org.nowstart.nyangnyangbot.adapter.in.scheduler.roulette;

import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.nowstart.nyangnyangbot.application.port.in.roulette.RecoverRouletteRunsUseCase;

class RouletteRecoverySchedulerTest {

    @Test
    void schedulerRecoversABoundedBatch() {
        RecoverRouletteRunsUseCase useCase = Mockito.mock(RecoverRouletteRunsUseCase.class);
        RouletteRecoveryScheduler scheduler = new RouletteRecoveryScheduler(useCase);

        scheduler.recoverPendingRuns();

        then(useCase).should().recoverPendingRuns(100);
    }
}
