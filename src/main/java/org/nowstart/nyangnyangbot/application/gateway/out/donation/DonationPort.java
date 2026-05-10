package org.nowstart.nyangnyangbot.application.gateway.out.donation;

import org.nowstart.nyangnyangbot.application.chzzk.dto.DonationDto;

public interface DonationPort {

    boolean existsByDonationEventId(String donationEventId);

    void save(DonationDto donation, Long payAmount, String emojisJson);
}
