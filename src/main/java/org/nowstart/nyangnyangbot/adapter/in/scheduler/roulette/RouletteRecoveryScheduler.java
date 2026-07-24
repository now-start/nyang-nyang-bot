package org.nowstart.nyangnyangbot.adapter.in.scheduler.roulette;

import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.roulette.RecoverRouletteRunsUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "nyang.roulette.recovery", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class RouletteRecoveryScheduler {

    private final RecoverRouletteRunsUseCase recoverRouletteRunsUseCase;

    @Scheduled(fixedDelayString = "${nyang.roulette.recovery.scheduler-delay-millis:30000}")
    public void recoverPendingRuns() {
        recoverRouletteRunsUseCase.recoverPendingRuns(RecoverRouletteRunsUseCase.DEFAULT_BATCH_SIZE);
    }
}
