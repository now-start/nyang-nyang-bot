package org.nowstart.nyangnyangbot.adapter.in.web.timer;

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

    @ModelAttribute("timerActiveOptions")
    public List<OptionView> timerActiveOptions() {
        return TIMER_ACTIVE_OPTIONS;
    }

    public record OptionView(String value, String label) {
    }
}
