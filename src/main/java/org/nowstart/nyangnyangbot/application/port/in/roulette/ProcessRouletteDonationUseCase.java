package org.nowstart.nyangnyangbot.application.port.in.roulette;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.port.in.donation.HandleDonationEventUseCase.DonationReceived;

public interface ProcessRouletteDonationUseCase {

    Optional<Long> processDonation(
            @NotNull(message = "donationId is required")
            @Positive(message = "donationId must be positive") Long donationId,
            @Valid @NotNull(message = "donation is required") DonationReceived donation
    );
}
