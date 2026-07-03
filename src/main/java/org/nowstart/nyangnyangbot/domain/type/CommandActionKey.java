package org.nowstart.nyangnyangbot.domain.type;

import java.util.Locale;

public enum CommandActionKey {
    FAVORITE_STATUS,
    ROULETTE_RESULT,
    ROULETTE_DONATION;

    public static CommandActionKey parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("actionKey is required");
        }
        return CommandActionKey.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
