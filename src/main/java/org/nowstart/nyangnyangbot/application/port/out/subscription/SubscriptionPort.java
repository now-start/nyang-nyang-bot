package org.nowstart.nyangnyangbot.application.port.out.subscription;

import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.SubscriptionEventPayload;

public interface SubscriptionPort {

    void save(SubscriptionEventPayload subscription);
}
