package org.nowstart.nyangnyangbot.application.port.in.command;

import java.util.Optional;

public interface ExecuteCommandUseCase {

    Optional<ApprovedCommand> execute(ExecuteCommand command);

    record ExecuteCommand(
            String trigger,
            String userId,
            String displayName,
            String args,
            String arg1,
            String arg2
    ) {
    }

    record ApprovedCommand(
            long commandId,
            String trigger,
            String renderedMessage,
            long totalCount,
            long userCount,
            int currentStreak,
            int longestStreak
    ) {
    }
}
