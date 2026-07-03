package org.nowstart.nyangnyangbot.domain.type;

import java.util.Locale;
import java.util.Optional;

public enum CommandActionKey {
    FAVORITE_STATUS("favorite"),
    ROULETTE_RESULT("roulette_result"),
    ROULETTE_DONATION(null);

    private final String commandBeanName;

    CommandActionKey(String commandBeanName) {
        this.commandBeanName = commandBeanName;
    }

    public Optional<String> commandBeanName() {
        return Optional.ofNullable(commandBeanName);
    }

    public static CommandActionKey parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("actionKey is required");
        }
        return CommandActionKey.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
