package org.nowstart.nyangnyangbot.domain.chat;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CommandTrigger {

    private static final int MAX_LENGTH = 20;

    private CommandTrigger() {
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }

    public static List<String> validationErrors(String value) {
        List<String> errors = new ArrayList<>();
        if (value == null) {
            errors.add("trigger is required");
            return errors;
        }
        if (!value.startsWith("!")) {
            errors.add("trigger must start with !");
        }
        if (value.length() < 2 || value.length() > MAX_LENGTH) {
            errors.add("trigger length must be between 2 and " + MAX_LENGTH);
        }
        if (value.chars().anyMatch(Character::isISOControl)) {
            errors.add("trigger must not contain control characters");
        }
        if (value.matches(".*\\s+.*")) {
            errors.add("trigger must be a single token");
        }
        return List.copyOf(errors);
    }

    public static void validate(String value) {
        List<String> errors = validationErrors(value);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
