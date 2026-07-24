package org.nowstart.nyangnyangbot.adapter.out.persistence.donation;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.donation.entity.Donation;
import org.nowstart.nyangnyangbot.adapter.out.persistence.donation.repository.DonationRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.application.port.out.donation.DonationPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DonationPersistenceAdapter implements DonationPort {

    private final DonationRepository donationRepository;
    private final EntityManager entityManager;
    private final OutboundContractValidator contractValidator;

    @Override
    @Transactional(readOnly = true)
    public Optional<DonationResult> findByIngestionKey(String ingestionKey) {
        return donationRepository.findByIngestionKey(ingestionKey).map(this::donationResult);
    }

    @Override
    @Transactional
    public DonationResult save(SaveDonationCommand command) {
        contractValidator.request("donation.save", command);
        Donation donation = Donation.builder()
                .ingestionKey(command.ingestionKey())
                .donationType(command.donationType())
                .recipientUserAccount(userReference(command.recipientUserId()))
                .donorUserAccount(userReference(command.donorUserId()))
                .donorDisplayName(command.donorDisplayName())
                .amount(command.amount())
                .message(command.message())
                .receivedAt(command.receivedAt())
                .build();
        return donationResult(donationRepository.save(donation));
    }

    private UserAccount userReference(String userId) {
        return userId == null || userId.isBlank()
                ? null
                : entityManager.getReference(UserAccount.class, userId);
    }

    private DonationResult donationResult(Donation donation) {
        return contractValidator.persistenceResult("donation.result", new DonationResult(
                donation.getId(),
                donation.getIngestionKey(),
                donation.getDonationType(),
                donation.getRecipientUserAccount().getUserId(),
                donation.getDonorUserAccount() == null ? null : donation.getDonorUserAccount().getUserId(),
                donation.getDonorDisplayName(),
                donation.getAmount(),
                donation.getMessage()
        ));
    }
}
