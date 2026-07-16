package org.nowstart.nyangnyangbot.adapter.in.scheduler.timer;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.timer.RunTimerMessagesUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;

@ExtendWith(MockitoExtension.class)
class TimerMessageSchedulerTest {

    @Mock
    private RunTimerMessagesUseCase runTimerMessagesUseCase;

    @Test
    void runDueTimerMessages_ShouldDelegateToUseCase() {
        TimerMessageScheduler scheduler = new TimerMessageScheduler(runTimerMessagesUseCase);

        scheduler.runDueTimerMessages();

        BDDMockito.then(runTimerMessagesUseCase).should().runDueTimerMessages();
    }

    @Test
    void scheduler_ShouldUseConfigurableThirtySecondDefaultAndBeEnabledByDefault() throws NoSuchMethodException {
        Scheduled scheduled = TimerMessageScheduler.class
                .getMethod("runDueTimerMessages")
                .getAnnotation(Scheduled.class);
        ConditionalOnProperty condition = TimerMessageScheduler.class.getAnnotation(ConditionalOnProperty.class);

        then(scheduled).isNotNull();
        then(scheduled.fixedDelayString()).isEqualTo("${nyang.timer.scheduler-delay-millis:30000}");
        then(condition).isNotNull();
        then(condition.prefix()).isEqualTo("nyang.timer");
        then(condition.name()).containsExactly("enabled");
        then(condition.havingValue()).isEqualTo("true");
        then(condition.matchIfMissing()).isTrue();
    }
}
