package org.nowstart.nyangnyangbot.application.port.out.subscription;

public interface SubscriptionPort {

    void save(SaveSubscriptionCommand command);

    record SaveSubscriptionCommand(
            String channelId,
            String subscriberChannelId,
            String subscriberNickname,
            Integer tierNo,
            String tierName,
            Integer month
    ) {
    }
}
