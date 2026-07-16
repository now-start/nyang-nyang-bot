package org.nowstart.nyangnyangbot.application.service.command;

import java.time.LocalDateTime;

public record CommandVariableContext(
        String userId,
        String nickname,
        String command,
        String args,
        String arg1,
        String arg2,
        LocalDateTime now
) {
}
