package org.nowstart.nyangnyangbot.application.dto.chzzk;

public record SubscriptionDto(
        String channelId,
        String subscriberChannelId,
        String subscriberNickname,
        Integer tierNo,
        String tierName,
        Integer month
) {
}
