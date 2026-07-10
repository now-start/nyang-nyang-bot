package org.nowstart.nyangnyangbot.application.service.donation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.DonationReceived;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ProcessRouletteDonationUseCase;
import org.nowstart.nyangnyangbot.application.port.out.donation.DonationPort;
import org.nowstart.nyangnyangbot.application.port.out.donation.DonationPort.SaveDonationCommand;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DonationService {

    private final DonationPort donationPort;
    private final ProcessRouletteDonationUseCase processRouletteDonationUseCase;

    public void handle(DonationReceived donation) {
        if (donation == null) {
            return;
        }
        log.info("[ChzzkDonation] socket received: {}", donation);
        if (isBlank(donation.donationEventId())
                || !donationPort.existsByDonationEventId(donation.donationEventId())) {
            donationPort.save(new SaveDonationCommand(
                    donation.donationEventId(),
                    donation.donationType(),
                    donation.channelId(),
                    donation.donatorChannelId(),
                    donation.donatorNickname(),
                    parseAmount(donation.payAmount()),
                    donation.donationText(),
                    donation.emojis()
            ));
        }
        processRouletteDonationUseCase.processDonation(donation);
    }

    private Long parseAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return 0L;
        }
        String digits = amount.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
