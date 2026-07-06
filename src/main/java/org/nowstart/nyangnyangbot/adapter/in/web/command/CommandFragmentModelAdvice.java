package org.nowstart.nyangnyangbot.adapter.in.web.command;

import java.util.List;
import org.nowstart.nyangnyangbot.adapter.in.web.favorite.FavoriteController;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = {FavoriteController.class, CommandFragmentController.class})
public class CommandFragmentModelAdvice {

    private static final List<CommandFragmentController.OptionView> COMMAND_TYPE_OPTIONS = List.of(
            new CommandFragmentController.OptionView("", "전체 유형"),
            new CommandFragmentController.OptionView("TEXT", "TEXT"),
            new CommandFragmentController.OptionView("TRIGGER", "TRIGGER"),
            new CommandFragmentController.OptionView("TIMER", "TIMER")
    );
    private static final List<CommandFragmentController.OptionView> COMMAND_EDITOR_TYPE_OPTIONS = COMMAND_TYPE_OPTIONS.stream()
            .filter(option -> !option.value().isBlank())
            .toList();
    private static final List<CommandFragmentController.OptionView> COMMAND_ACTIVE_OPTIONS = List.of(
            new CommandFragmentController.OptionView("", "전체 상태"),
            new CommandFragmentController.OptionView("true", "활성"),
            new CommandFragmentController.OptionView("false", "비활성")
    );
    private static final List<CommandFragmentController.OptionView> ACTION_OPTIONS = List.of(
            new CommandFragmentController.OptionView("", "없음"),
            new CommandFragmentController.OptionView("FAVORITE_STATUS", "FAVORITE_STATUS"),
            new CommandFragmentController.OptionView("ROULETTE_RESULT", "ROULETTE_RESULT"),
            new CommandFragmentController.OptionView("ROULETTE_DONATION", "ROULETTE_DONATION")
    );
    private static final List<CommandFragmentController.OptionView> ROLE_OPTIONS = List.of(
            new CommandFragmentController.OptionView("USER", "USER"),
            new CommandFragmentController.OptionView("ADMIN", "ADMIN")
    );

    @ModelAttribute("commandTypeOptions")
    public List<CommandFragmentController.OptionView> commandTypeOptions() {
        return COMMAND_TYPE_OPTIONS;
    }

    @ModelAttribute("commandEditorTypeOptions")
    public List<CommandFragmentController.OptionView> commandEditorTypeOptions() {
        return COMMAND_EDITOR_TYPE_OPTIONS;
    }

    @ModelAttribute("commandActiveOptions")
    public List<CommandFragmentController.OptionView> commandActiveOptions() {
        return COMMAND_ACTIVE_OPTIONS;
    }

    @ModelAttribute("commandActionOptions")
    public List<CommandFragmentController.OptionView> commandActionOptions() {
        return ACTION_OPTIONS;
    }

    @ModelAttribute("commandRoleOptions")
    public List<CommandFragmentController.OptionView> commandRoleOptions() {
        return ROLE_OPTIONS;
    }
}
