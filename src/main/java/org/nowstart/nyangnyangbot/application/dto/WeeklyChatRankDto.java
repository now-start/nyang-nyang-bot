package org.nowstart.nyangnyangbot.application.dto;

public record WeeklyChatRankDto(
        Integer rank,
        String nickname,
        Long chatCount
) {
}
