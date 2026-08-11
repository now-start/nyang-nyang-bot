package org.nowstart.nyangnyangbot.application.port.in.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;

public interface ExecuteCommandUseCase {

    Optional<ApprovedCommand> execute(
            @Valid @NotNull(message = "command is required") ExecuteCommand command
    );

    record ExecuteCommand(
            @NotBlank(message = "trigger is required") String trigger,
            @NotBlank(message = "userId is required") String userId,
            String displayName,
            String args,
            String arg1,
            String arg2
    ) {
    }

    record ApprovedCommand(
            long commandId,
            String trigger,
            String renderedMessage
    ) {
    }
}
