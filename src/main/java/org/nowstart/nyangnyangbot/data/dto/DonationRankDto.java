package org.nowstart.nyangnyangbot.data.dto;

public record DonationRankDto(
        int rank,
        String nickname,
        long totalAmount
) {
}
