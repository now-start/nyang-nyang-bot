package org.nowstart.nyangnyangbot.adapter.in.web.command;

import java.util.List;
import org.nowstart.nyangnyangbot.adapter.in.web.favorite.FavoriteController;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = {FavoriteController.class, CommandController.class})
public class CommandModelAdvice {

    private static final List<CommandController.OptionView> COMMAND_TYPE_OPTIONS = List.of(
            new CommandController.OptionView("", "전체 유형"),
            new CommandController.OptionView("TEXT", "TEXT"),
            new CommandController.OptionView("TRIGGER", "TRIGGER"),
            new CommandController.OptionView("TIMER", "TIMER")
    );
    private static final List<CommandController.OptionView> COMMAND_EDITOR_TYPE_OPTIONS = COMMAND_TYPE_OPTIONS.stream()
            .filter(option -> !option.value().isBlank())
            .toList();
    private static final List<CommandController.OptionView> COMMAND_ACTIVE_OPTIONS = List.of(
            new CommandController.OptionView("", "전체 상태"),
            new CommandController.OptionView("true", "활성"),
            new CommandController.OptionView("false", "비활성")
    );
    private static final List<CommandController.OptionView> ACTION_OPTIONS = List.of(
            new CommandController.OptionView("", "없음"),
            new CommandController.OptionView("FAVORITE_STATUS", "FAVORITE_STATUS"),
            new CommandController.OptionView("ROULETTE_RESULT", "ROULETTE_RESULT"),
            new CommandController.OptionView("ROULETTE_DONATION", "ROULETTE_DONATION")
    );
    private static final List<CommandController.OptionView> ROLE_OPTIONS = List.of(
            new CommandController.OptionView("USER", "USER"),
            new CommandController.OptionView("ADMIN", "ADMIN")
    );

    @ModelAttribute("commandTypeOptions")
    public List<CommandController.OptionView> commandTypeOptions() {
        return COMMAND_TYPE_OPTIONS;
    }

    @ModelAttribute("commandEditorTypeOptions")
    public List<CommandController.OptionView> commandEditorTypeOptions() {
        return COMMAND_EDITOR_TYPE_OPTIONS;
    }

    @ModelAttribute("commandActiveOptions")
    public List<CommandController.OptionView> commandActiveOptions() {
        return COMMAND_ACTIVE_OPTIONS;
    }

    @ModelAttribute("commandActionOptions")
    public List<CommandController.OptionView> commandActionOptions() {
        return ACTION_OPTIONS;
    }

    @ModelAttribute("commandRoleOptions")
    public List<CommandController.OptionView> commandRoleOptions() {
        return ROLE_OPTIONS;
    }
}
