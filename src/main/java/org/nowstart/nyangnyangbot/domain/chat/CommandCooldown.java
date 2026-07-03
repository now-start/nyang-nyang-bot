package org.nowstart.nyangnyangbot.domain.chat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
        Long previous = lastCommandTimes.get(key);
        if (previous != null && currentTimeMillis - previous < cooldownMillis) {
            return true;
        }
        lastCommandTimes.put(key, currentTimeMillis);
        return false;
    }

    public Map<String, Long> snapshot() {
        return Map.copyOf(lastCommandTimes);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
