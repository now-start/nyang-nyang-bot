package org.nowstart.nyangnyangbot.domain.command;

public final class CommandPolicy {

    public static final int MAX_TEMPLATE_LENGTH = 1_000;
    public static final CommandExecutionPolicy DEFAULT_EXECUTION_POLICY = CommandExecutionPolicy.USER_INTERVAL;
    public static final int DEFAULT_USER_COOLDOWN_SECONDS = 30;
    public static final int MIN_USER_COOLDOWN_SECONDS = 5;
    public static final int MAX_USER_COOLDOWN_SECONDS = 3_600;
    public static final String TEMPLATE_LENGTH_MESSAGE =
            "messageTemplate length must be " + MAX_TEMPLATE_LENGTH + " or less";
    public static final String USER_COOLDOWN_RANGE_MESSAGE = "userCooldownSeconds must be between "
            + MIN_USER_COOLDOWN_SECONDS + " and " + MAX_USER_COOLDOWN_SECONDS;

    private CommandPolicy() {
    }
}
