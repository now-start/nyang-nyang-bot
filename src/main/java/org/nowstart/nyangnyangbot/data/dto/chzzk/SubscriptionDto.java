package org.nowstart.nyangnyangbot.data.dto.chzzk;

public record SubscriptionDto(
        String channelId,
        String subscriberChannelId,
        String subscriberNickname,
        Integer tierNo,
        String tierName,
        Integer month
) {
}
