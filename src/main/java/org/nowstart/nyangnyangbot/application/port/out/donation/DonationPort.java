package org.nowstart.nyangnyangbot.application.port.out.donation;

import org.nowstart.nyangnyangbot.application.dto.chzzk.DonationDto;

public interface DonationPort {

    boolean existsByDonationEventId(String donationEventId);

    void save(DonationDto donation, Long payAmount, String emojisJson);
}
