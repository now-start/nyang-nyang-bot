package org.nowstart.nyangnyangbot.adapter.in.web.command;

import java.util.List;
import org.nowstart.nyangnyangbot.adapter.in.web.point.PointController;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = {PointController.class, CommandController.class})
public class CommandModelAdvice {

    private static final List<CommandController.OptionView> COMMAND_ACTIVE_OPTIONS = List.of(
            new CommandController.OptionView("", "전체 상태"),
            new CommandController.OptionView("true", "활성"),
            new CommandController.OptionView("false", "비활성")
    );
    private static final List<CommandController.OptionView> COMMAND_EXECUTION_POLICY_OPTIONS = List.of(
            new CommandController.OptionView("USER_INTERVAL", "사용자별 시간 간격"),
            new CommandController.OptionView("USER_CALENDAR_DAY", "사용자별 하루 1회")
    );

    @ModelAttribute("commandActiveOptions")
    public List<CommandController.OptionView> commandActiveOptions() {
        return COMMAND_ACTIVE_OPTIONS;
    }

    @ModelAttribute("commandExecutionPolicyOptions")
    public List<CommandController.OptionView> commandExecutionPolicyOptions() {
        return COMMAND_EXECUTION_POLICY_OPTIONS;
    }

}
