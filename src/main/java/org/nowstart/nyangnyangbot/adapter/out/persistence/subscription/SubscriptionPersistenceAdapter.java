package org.nowstart.nyangnyangbot.adapter.out.persistence.subscription;

import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.gateway.out.subscription.SubscriptionPort;
import org.nowstart.nyangnyangbot.application.dto.chzzk.SubscriptionDto;
import org.nowstart.nyangnyangbot.adapter.out.persistence.entity.SubscriptionEntity;
import org.nowstart.nyangnyangbot.adapter.out.persistence.repository.SubscriptionRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionPersistenceAdapter implements SubscriptionPort {

    private final SubscriptionRepository subscriptionRepository;

    @Override
    public void save(SubscriptionDto subscription) {
        subscriptionRepository.save(SubscriptionEntity.builder()
                .channelId(subscription.channelId())
                .subscriberChannelId(subscription.subscriberChannelId())
                .subscriberNickname(subscription.subscriberNickname())
                .tierNo(subscription.tierNo())
                .tierName(subscription.tierName())
                .month(subscription.month())
                .build());
    }
}
