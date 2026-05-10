package org.nowstart.nyangnyangbot.application.dto.chzzk;

public record SystemDto(String type, SystemData data) {

    public record SystemData(String sessionKey, String eventType, String channelId) {
    }
}
