package org.nowstart.nyangnyangbot.application.service.subscription;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.SubscriptionReceived;
import org.nowstart.nyangnyangbot.application.port.out.subscription.SubscriptionPort;
import org.nowstart.nyangnyangbot.application.port.out.subscription.SubscriptionPort.SaveSubscriptionCommand;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionPort subscriptionPort;

    public void handle(SubscriptionReceived subscription) {
        if (subscription == null) {
            return;
        }
        log.info("[ChzzkSubscription] socket received: {}", subscription);
        subscriptionPort.save(new SaveSubscriptionCommand(
                subscription.channelId(),
                subscription.subscriberChannelId(),
                subscription.subscriberNickname(),
                subscription.tierNo(),
                subscription.tierName(),
                subscription.month()
        ));
    }
}
