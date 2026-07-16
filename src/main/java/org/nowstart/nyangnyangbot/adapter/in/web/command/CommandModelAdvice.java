package org.nowstart.nyangnyangbot.adapter.in.web.command;

import java.util.List;
import org.nowstart.nyangnyangbot.adapter.in.web.favorite.FavoriteController;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = {FavoriteController.class, CommandController.class})
public class CommandModelAdvice {

    private static final List<CommandController.OptionView> COMMAND_ACTIVE_OPTIONS = List.of(
            new CommandController.OptionView("", "전체 상태"),
            new CommandController.OptionView("true", "활성"),
            new CommandController.OptionView("false", "비활성")
    );

    @ModelAttribute("commandActiveOptions")
    public List<CommandController.OptionView> commandActiveOptions() {
        return COMMAND_ACTIVE_OPTIONS;
    }

}
