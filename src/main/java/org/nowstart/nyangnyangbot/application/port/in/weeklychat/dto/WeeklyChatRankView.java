package org.nowstart.nyangnyangbot.application.port.in.weeklychat.dto;

public record WeeklyChatRankView(
        Integer rank,
        String nickname,
        Long chatCount
) {
}
