package org.nowstart.nyangnyangbot.data.dto.chzzk;

public record SystemDto(String type, SystemData data) {

    public record SystemData(String sessionKey, String eventType, String channelId) {
    }
}
