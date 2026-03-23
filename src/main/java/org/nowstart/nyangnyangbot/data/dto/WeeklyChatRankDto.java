package org.nowstart.nyangnyangbot.data.dto;

public record WeeklyChatRankDto(
        Integer rank,
        String nickname,
        Long chatCount
) {
}
