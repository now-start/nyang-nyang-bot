package org.nowstart.nyangnyangbot.application.service.roulette;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.roulette.RecoverRouletteRunsUseCase;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RouletteRunPreparedListener {

    private final RecoverRouletteRunsUseCase recoverRouletteRunsUseCase;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void recoverPreparedRun(RouletteRunPreparedEvent event) {
        try {
            recoverRouletteRunsUseCase.recoverRun(event.runId());
        } catch (RuntimeException failure) {
            log.warn("action=roulette.after_commit result=retry_later runId={}", event.runId(), failure);
        }
    }
}
