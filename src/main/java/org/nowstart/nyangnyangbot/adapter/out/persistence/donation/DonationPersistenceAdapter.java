package org.nowstart.nyangnyangbot.adapter.out.persistence.donation;

import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.out.donation.DonationPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.DonationEventPayload;
import org.nowstart.nyangnyangbot.adapter.out.persistence.donation.entity.Donation;
import org.nowstart.nyangnyangbot.adapter.out.persistence.donation.repository.DonationRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DonationPersistenceAdapter implements DonationPort {

    private final DonationRepository donationRepository;

    @Override
    public boolean existsByDonationEventId(String donationEventId) {
        return donationRepository.existsByDonationEventId(donationEventId);
    }

    @Override
    public void save(DonationEventPayload donation, Long payAmount, String emojisJson) {
        donationRepository.save(Donation.builder()
                .donationEventId(normalizeDonationEventId(donation.donationEventId()))
                .donationType(donation.donationType())
                .channelId(donation.channelId())
                .donatorChannelId(donation.donatorChannelId())
                .donatorNickname(donation.donatorNickname())
                .payAmount(payAmount)
                .donationText(donation.donationText())
                .emojisJson(emojisJson)
                .build());
    }

    private String normalizeDonationEventId(String donationEventId) {
        if (donationEventId == null || donationEventId.isBlank()) {
            return null;
        }
        return donationEventId;
    }
}
