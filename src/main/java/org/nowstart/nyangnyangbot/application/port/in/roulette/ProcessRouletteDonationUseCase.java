package org.nowstart.nyangnyangbot.application.port.in.roulette;

import java.util.Optional;
import org.nowstart.nyangnyangbot.application.port.in.donation.HandleDonationEventUseCase.DonationReceived;

public interface ProcessRouletteDonationUseCase {

    Optional<Long> processDonation(Long donationId, DonationReceived donation);
}
