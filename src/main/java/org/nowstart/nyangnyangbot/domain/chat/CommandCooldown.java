package org.nowstart.nyangnyangbot.domain.chat;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandCooldown {

    private final long cooldownMillis;
    private final ConcurrentMap<String, Long> lastCommandTimes = new ConcurrentHashMap<>();

    public CommandCooldown(long cooldownMillis) {
        if (cooldownMillis < 0) {
            throw new IllegalArgumentException("cooldownMillis must not be negative");
        }
        this.cooldownMillis = cooldownMillis;
    }

    public boolean isInCooldown(String userId, String commandName, long currentTimeMillis) {
        return isInCooldown(userId, commandName, currentTimeMillis, cooldownMillis);
    }

    public boolean isInCooldown(String userId, String commandName, long currentTimeMillis, long cooldownMillis) {
        if (cooldownMillis < 0) {
            throw new IllegalArgumentException("cooldownMillis must not be negative");
        }
        if (isBlank(userId) || isBlank(commandName)) {
            return false;
        }
        String key = userId + ":" + commandName;
        AtomicBoolean inCooldown = new AtomicBoolean(false);
        lastCommandTimes.compute(key, (ignored, previous) -> {
            if (previous != null && currentTimeMillis - previous < cooldownMillis) {
                inCooldown.set(true);
                return previous;
            }
            return currentTimeMillis;
        });
        return inCooldown.get();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
