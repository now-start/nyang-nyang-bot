package org.nowstart.nyangnyangbot.adapter.in.scheduler.timer;

import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.timer.RunTimerMessagesUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "nyang.timer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TimerMessageScheduler {

    private final RunTimerMessagesUseCase runTimerMessagesUseCase;

    @Scheduled(fixedDelayString = "${nyang.timer.scheduler-delay-millis:30000}")
    public void runDueTimerMessages() {
        runTimerMessagesUseCase.runDueTimerMessages();
    }
}
