package org.nowstart.nyangnyangbot.application.port.out.donation;

import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.DonationEventPayload;

public interface DonationPort {

    boolean existsByDonationEventId(String donationEventId);

    void save(DonationEventPayload donation, Long payAmount, String emojisJson);
}
