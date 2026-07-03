package org.nowstart.nyangnyangbot.domain.type;

import java.util.Locale;

public enum CommandType {
    TEXT,
    TRIGGER,
    TIMER;

    public static CommandType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        return CommandType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
