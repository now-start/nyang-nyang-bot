package org.nowstart.nyangnyangbot.adapter.in.web.timer;

import static org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.MAX_CHAT_COUNT;
import static org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.MAX_INTERVAL_MINUTES;
import static org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.MAX_TEMPLATE_LENGTH;
import static org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.MIN_CHAT_COUNT;
import static org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.MIN_INTERVAL_MINUTES;

import java.util.List;
import org.nowstart.nyangnyangbot.adapter.in.web.point.PointController;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = {PointController.class, TimerMessageController.class})
public class TimerMessageModelAdvice {

    private static final List<OptionView> TIMER_ACTIVE_OPTIONS = List.of(
            new OptionView("", "전체 상태"),
            new OptionView("true", "활성"),
            new OptionView("false", "비활성")
    );
    private static final TimerMessageConstraintsView TIMER_MESSAGE_CONSTRAINTS = new TimerMessageConstraintsView(
            MIN_INTERVAL_MINUTES,
            MAX_INTERVAL_MINUTES,
            MIN_CHAT_COUNT,
            MAX_CHAT_COUNT,
            MAX_TEMPLATE_LENGTH
    );

    @ModelAttribute("timerActiveOptions")
    public List<OptionView> timerActiveOptions() {
        return TIMER_ACTIVE_OPTIONS;
    }

    @ModelAttribute("timerMessageConstraints")
    public TimerMessageConstraintsView timerMessageConstraints() {
        return TIMER_MESSAGE_CONSTRAINTS;
    }

    public record OptionView(String value, String label) {
    }

    public record TimerMessageConstraintsView(
            int minIntervalMinutes,
            int maxIntervalMinutes,
            int minChatCount,
            int maxChatCount,
            int maxTemplateLength
    ) {
    }
}
