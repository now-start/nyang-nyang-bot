package org.nowstart.nyangnyangbot.application.weeklychat.dto;

public record WeeklyChatRankDto(
        Integer rank,
        String nickname,
        Long chatCount
) {
}
