package org.nowstart.nyangnyangbot.data.dto.chzzk;

public record SubscriptionDto(
        String channelId,
        String subscriberChannelId,
        String subscriberNickname,
        int tierNo,
        String tierName,
        int month
) {
}
