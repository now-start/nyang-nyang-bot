package org.nowstart.nyangnyangbot.domain.chat;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CommandTrigger {

    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 20;
    public static final String LENGTH_MESSAGE =
            "trigger length must be between " + MIN_LENGTH + " and " + MAX_LENGTH;

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
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            errors.add(LENGTH_MESSAGE);
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
