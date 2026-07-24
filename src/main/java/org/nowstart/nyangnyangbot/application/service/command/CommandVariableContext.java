package org.nowstart.nyangnyangbot.application.service.command;

import java.time.Instant;

public record CommandVariableContext(
        String userId,
        String nickname,
        String command,
        String args,
        String arg1,
        String arg2,
        Instant now,
        long totalCount,
        long userCount,
        int currentStreak,
        int longestStreak
) {
    public CommandVariableContext(
            String userId,
            String nickname,
            String command,
            String args,
            String arg1,
            String arg2,
            Instant now
    ) {
        this(userId, nickname, command, args, arg1, arg2, now, 0, 0, 0, 0);
    }
}
