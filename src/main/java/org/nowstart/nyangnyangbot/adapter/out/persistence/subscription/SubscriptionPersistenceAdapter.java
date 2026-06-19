package org.nowstart.nyangnyangbot.adapter.out.persistence.subscription;

import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.out.subscription.SubscriptionPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.SubscriptionEventPayload;
import org.nowstart.nyangnyangbot.adapter.out.persistence.subscription.entity.Subscription;
import org.nowstart.nyangnyangbot.adapter.out.persistence.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionPersistenceAdapter implements SubscriptionPort {

    private final SubscriptionRepository subscriptionRepository;

    @Override
    public void save(SubscriptionEventPayload subscription) {
        subscriptionRepository.save(Subscription.builder()
                .channelId(subscription.channelId())
                .subscriberChannelId(subscription.subscriberChannelId())
                .subscriberNickname(subscription.subscriberNickname())
                .tierNo(subscription.tierNo())
                .tierName(subscription.tierName())
                .subscriptionMonth(subscription.month())
                .build());
    }
}
