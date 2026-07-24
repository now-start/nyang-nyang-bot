package org.nowstart.nyangnyangbot.adapter.in.web.command;

import static org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.CALENDAR_DAY_EXECUTION_POLICY;
import static org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.DEFAULT_EXECUTION_POLICY;
import static org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.DEFAULT_USER_COOLDOWN_SECONDS;
import static org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.MAX_TRIGGER_LENGTH;
import static org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.MAX_TEMPLATE_LENGTH;
import static org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.MAX_USER_COOLDOWN_SECONDS;
import static org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.MIN_USER_COOLDOWN_SECONDS;

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
            new CommandController.OptionView(DEFAULT_EXECUTION_POLICY.name(), "사용자별 시간 간격"),
            new CommandController.OptionView(CALENDAR_DAY_EXECUTION_POLICY.name(), "사용자별 하루 1회")
    );
    private static final CommandConstraintsView COMMAND_CONSTRAINTS = new CommandConstraintsView(
            MAX_TRIGGER_LENGTH,
            MAX_TEMPLATE_LENGTH,
            DEFAULT_USER_COOLDOWN_SECONDS,
            MIN_USER_COOLDOWN_SECONDS,
            MAX_USER_COOLDOWN_SECONDS
    );

    @ModelAttribute("commandActiveOptions")
    public List<CommandController.OptionView> commandActiveOptions() {
        return COMMAND_ACTIVE_OPTIONS;
    }

    @ModelAttribute("commandExecutionPolicyOptions")
    public List<CommandController.OptionView> commandExecutionPolicyOptions() {
        return COMMAND_EXECUTION_POLICY_OPTIONS;
    }

    @ModelAttribute("commandConstraints")
    public CommandConstraintsView commandConstraints() {
        return COMMAND_CONSTRAINTS;
    }

    public record CommandConstraintsView(
            int maxTriggerLength,
            int maxTemplateLength,
            int defaultUserCooldownSeconds,
            int minUserCooldownSeconds,
            int maxUserCooldownSeconds
    ) {
    }
}
