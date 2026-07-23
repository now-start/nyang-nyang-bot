package org.nowstart.nyangnyangbot.application.service.roulette;

import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.nowstart.nyangnyangbot.application.port.in.roulette.RecoverRouletteRunsUseCase;

class RouletteRunPreparedListenerTest {

    @Test
    void recoversPreparedRunAfterDonationTransactionCommits() {
        RecoverRouletteRunsUseCase useCase = Mockito.mock(RecoverRouletteRunsUseCase.class);
        RouletteRunPreparedListener listener = new RouletteRunPreparedListener(useCase);

        listener.recoverPreparedRun(new RouletteRunPreparedEvent(7L));

        then(useCase).should().recoverRun(7L);
    }

    @Test
    void leavesFailureForScheduledRecovery() {
        RecoverRouletteRunsUseCase useCase = Mockito.mock(RecoverRouletteRunsUseCase.class);
        RouletteRunPreparedListener listener = new RouletteRunPreparedListener(useCase);
        Mockito.doThrow(new IllegalStateException("retry")).when(useCase).recoverRun(7L);

        listener.recoverPreparedRun(new RouletteRunPreparedEvent(7L));

        then(useCase).should().recoverRun(7L);
    }
}
