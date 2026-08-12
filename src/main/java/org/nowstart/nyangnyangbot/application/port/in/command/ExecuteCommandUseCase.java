package org.nowstart.nyangnyangbot.application.port.in.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;

public interface ExecuteCommandUseCase {

    /** 실행 가능한 명령어를 수행하고 승인된 응답을 반환하며, 실행이 거부되면 빈 값을 반환한다. */
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
