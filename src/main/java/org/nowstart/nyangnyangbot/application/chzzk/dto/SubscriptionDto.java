package org.nowstart.nyangnyangbot.application.chzzk.dto;

public record SubscriptionDto(
        String channelId,
        String subscriberChannelId,
        String subscriberNickname,
        Integer tierNo,
        String tierName,
        Integer month
) {
}
