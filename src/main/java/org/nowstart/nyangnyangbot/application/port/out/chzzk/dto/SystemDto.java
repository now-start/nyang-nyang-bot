package org.nowstart.nyangnyangbot.application.port.out.chzzk.dto;

public record SystemDto(String type, SystemData data) {

    public record SystemData(String sessionKey, String eventType, String channelId) {
    }
}
