package org.nowstart.nyangnyangbot.adapter.out.persistence.donation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.donation.entity.Donation;
import org.nowstart.nyangnyangbot.adapter.out.persistence.donation.repository.DonationRepository;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.application.port.out.donation.DonationPort;
import org.nowstart.nyangnyangbot.application.port.out.donation.DonationPort.SaveDonationCommand;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DonationPersistenceAdapter implements DonationPort {

    private final DonationRepository donationRepository;
    private final ObjectMapper objectMapper;
    private final OutboundContractValidator contractValidator;

    @Override
    public boolean existsByDonationEventId(String donationEventId) {
        return donationRepository.existsByDonationEventId(donationEventId);
    }

    @Override
    public void save(SaveDonationCommand command) {
        contractValidator.request("donation.save", command);
        donationRepository.save(Donation.builder()
                .donationEventId(normalizeDonationEventId(command.donationEventId()))
                .donationType(command.donationType())
                .channelId(command.channelId())
                .donatorChannelId(command.donatorChannelId())
                .donatorNickname(command.donatorNickname())
                .payAmount(command.payAmount())
                .donationText(command.donationText())
                .emojisJson(toJson(command.emojis()))
                .build());
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize donation emojis", ex);
        }
    }

    private String normalizeDonationEventId(String donationEventId) {
        if (donationEventId == null || donationEventId.isBlank()) {
            return null;
        }
        return donationEventId;
    }
}
