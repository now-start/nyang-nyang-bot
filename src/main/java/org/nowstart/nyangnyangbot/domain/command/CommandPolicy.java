package org.nowstart.nyangnyangbot.domain.command;

public final class CommandPolicy {

    public static final int MAX_TEMPLATE_LENGTH = 1_000;
    public static final CommandExecutionPolicy DEFAULT_EXECUTION_POLICY = CommandExecutionPolicy.USER_INTERVAL;
    public static final int DEFAULT_USER_COOLDOWN_SECONDS = 30;
    public static final int MIN_USER_COOLDOWN_SECONDS = 5;
    public static final int MAX_USER_COOLDOWN_SECONDS = 3_600;

    private CommandPolicy() {
    }
}
