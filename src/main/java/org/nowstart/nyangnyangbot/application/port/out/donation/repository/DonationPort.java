package org.nowstart.nyangnyangbot.application.port.out.donation.repository;

import org.nowstart.nyangnyangbot.application.port.out.chzzk.dto.DonationDto;

public interface DonationPort {

    boolean existsByDonationEventId(String donationEventId);

    void save(DonationDto donation, Long payAmount, String emojisJson);
}
