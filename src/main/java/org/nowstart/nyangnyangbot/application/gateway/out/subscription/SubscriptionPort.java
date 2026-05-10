package org.nowstart.nyangnyangbot.application.gateway.out.subscription;

import org.nowstart.nyangnyangbot.application.chzzk.dto.SubscriptionDto;

public interface SubscriptionPort {

    void save(SubscriptionDto subscription);
}
