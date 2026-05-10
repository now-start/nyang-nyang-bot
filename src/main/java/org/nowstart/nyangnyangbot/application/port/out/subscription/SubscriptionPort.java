package org.nowstart.nyangnyangbot.application.port.out.subscription;

import org.nowstart.nyangnyangbot.application.dto.chzzk.SubscriptionDto;

public interface SubscriptionPort {

    void save(SubscriptionDto subscription);
}
