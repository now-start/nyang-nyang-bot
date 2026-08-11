package org.nowstart.nyangnyangbot.application.validation;

import org.nowstart.nyangnyangbot.domain.command.CommandPolicy;

public final class CommandValidationMessages {

    public static final String TEMPLATE_LENGTH_MESSAGE =
            "messageTemplate length must be " + CommandPolicy.MAX_TEMPLATE_LENGTH + " or less";
    public static final String USER_COOLDOWN_RANGE_MESSAGE = "userCooldownSeconds must be between "
            + CommandPolicy.MIN_USER_COOLDOWN_SECONDS + " and " + CommandPolicy.MAX_USER_COOLDOWN_SECONDS;

    private CommandValidationMessages() {
    }
}
