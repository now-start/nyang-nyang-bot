package org.nowstart.nyangnyangbot.application.port.out.subscription;

import org.nowstart.nyangnyangbot.data.dto.chzzk.SubscriptionDto;

public interface SubscriptionPort {

    void save(SubscriptionDto subscription);
}
