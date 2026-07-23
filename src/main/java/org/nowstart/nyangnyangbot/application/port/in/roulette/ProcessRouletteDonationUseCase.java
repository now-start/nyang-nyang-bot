package org.nowstart.nyangnyangbot.application.port.in.roulette;

import java.util.Optional;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.DonationReceived;

public interface ProcessRouletteDonationUseCase {

    Optional<Long> processDonation(Long donationId, DonationReceived donation);
}
