package org.nowstart.nyangnyangbot.application.port.out.chzzk.dto;

public record SubscriptionDto(
        String channelId,
        String subscriberChannelId,
        String subscriberNickname,
        Integer tierNo,
        String tierName,
        Integer month
) {
}
