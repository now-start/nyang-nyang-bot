package org.nowstart.nyangnyangbot.application.chzzk.dto;

public record SystemDto(String type, SystemData data) {

    public record SystemData(String sessionKey, String eventType, String channelId) {
    }
}
