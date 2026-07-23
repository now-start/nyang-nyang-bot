package org.nowstart.nyangnyangbot.domain.presence;

public record PresenceUserState(String userId, String displayName, long lastMessageTime) {
}
