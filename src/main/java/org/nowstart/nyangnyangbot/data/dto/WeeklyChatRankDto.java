package org.nowstart.nyangnyangbot.data.dto;

public record WeeklyChatRankDto(
        int rank,
        String nickname,
        long chatCount
) {
}
