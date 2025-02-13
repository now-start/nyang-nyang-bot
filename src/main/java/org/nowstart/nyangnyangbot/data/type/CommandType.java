package org.nowstart.nyangnyangbot.data.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;

@Getter
@RequiredArgsConstructor
public enum CommandType {
    FAVORITE("!호감도");

    private final String command;

    public static String findNameByCommand(String command) {
        for (CommandType cmd : CommandType.values()) {
            if (cmd.getCommand().equals(command)) {
                return StringUtils.lowerCase(cmd.name());
            }
        }
        return null;
    }
}
