package org.nowstart.nyangnyangbot.domain.timer;

public final class TimerMessagePolicy {

    public static final int MAX_TEMPLATE_LENGTH = 1_000;
    public static final int MIN_INTERVAL_MINUTES = 5;
    public static final int MAX_INTERVAL_MINUTES = 1_440;
    public static final int MIN_CHAT_COUNT = 1;
    public static final int MAX_CHAT_COUNT = 10_000;
    public static final String TEMPLATE_LENGTH_MESSAGE =
            "messageTemplate length must be " + MAX_TEMPLATE_LENGTH + " or less";
    public static final String INTERVAL_RANGE_MESSAGE = "intervalMinutes must be between "
            + MIN_INTERVAL_MINUTES + " and " + MAX_INTERVAL_MINUTES;
    public static final String CHAT_COUNT_RANGE_MESSAGE =
            "minChatCount must be between " + MIN_CHAT_COUNT + " and " + MAX_CHAT_COUNT;

    private TimerMessagePolicy() {
    }
}
