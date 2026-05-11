package org.nowstart.nyangnyangbot.application.port.out.subscription.repository;

import org.nowstart.nyangnyangbot.application.port.out.chzzk.dto.SubscriptionDto;

public interface SubscriptionPort {

    void save(SubscriptionDto subscription);
}
