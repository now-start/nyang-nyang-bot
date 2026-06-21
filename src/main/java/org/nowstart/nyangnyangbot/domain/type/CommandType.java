package org.nowstart.nyangnyangbot.domain.type;

import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommandType {
    FAVORITE("!호감도"),
    ROULETTE_RESULT("!룰렛결과");

    private final String command;

    public static String findNameByCommand(String command) {
        for (CommandType cmd : CommandType.values()) {
            if (cmd.getCommand().equals(command)) {
                return cmd.name().toLowerCase(Locale.ROOT);
            }
        }
        return null;
    }
}
