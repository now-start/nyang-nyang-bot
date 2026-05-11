package org.nowstart.nyangnyangbot.adapter.out.persistence.donation;

import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.out.donation.repository.DonationPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.dto.DonationDto;
import org.nowstart.nyangnyangbot.adapter.out.persistence.entity.DonationEntity;
import org.nowstart.nyangnyangbot.adapter.out.persistence.repository.DonationRepository;
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
    public void save(DonationDto donation, Long payAmount, String emojisJson) {
        donationRepository.save(DonationEntity.builder()
                .donationEventId(donation.donationEventId())
                .donationType(donation.donationType())
                .channelId(donation.channelId())
                .donatorChannelId(donation.donatorChannelId())
                .donatorNickname(donation.donatorNickname())
                .payAmount(payAmount)
                .donationText(donation.donationText())
                .emojisJson(emojisJson)
                .build());
    }
}
