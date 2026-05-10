package org.nowstart.nyangnyangbot.application.gateway.out.subscription;

import org.nowstart.nyangnyangbot.application.dto.chzzk.SubscriptionDto;

public interface SubscriptionPort {

    void save(SubscriptionDto subscription);
}
