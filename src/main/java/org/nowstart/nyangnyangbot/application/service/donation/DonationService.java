package org.nowstart.nyangnyangbot.application.service.donation;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.DonationReceived;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ProcessRouletteDonationUseCase;
import org.nowstart.nyangnyangbot.application.port.in.user.ObserveUserUseCase;
import org.nowstart.nyangnyangbot.application.port.out.donation.DonationPort;
import org.nowstart.nyangnyangbot.application.port.out.donation.DonationPort.DonationResult;
import org.nowstart.nyangnyangbot.application.port.out.donation.DonationPort.SaveDonationCommand;
import org.nowstart.nyangnyangbot.application.service.roulette.RouletteRunPreparedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DonationService {

    private final DonationPort donationPort;
    private final ObserveUserUseCase observeUserUseCase;
    private final ProcessRouletteDonationUseCase processRouletteDonationUseCase;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void handle(DonationReceived donation) {
        if (donation == null || isBlank(donation.ingestionKey()) || isBlank(donation.channelId())) {
            return;
        }
        log.info("[ChzzkDonation] socket received: {}", donation);
        DonationResult existing = donationPort.findByIngestionKey(donation.ingestionKey()).orElse(null);
        if (existing != null) {
            publishPreparedRun(processRouletteDonationUseCase.processDonation(
                    existing.id(), canonicalEvent(existing)));
            return;
        }
        observeUserUseCase.observeUser(donation.channelId(), null);
        String donorUserId = normalize(donation.donatorChannelId());
        if (donorUserId != null) {
            observeUserUseCase.observeUser(donorUserId, donation.donatorNickname());
        }
        DonationResult persisted = create(donation, donorUserId);
        publishPreparedRun(processRouletteDonationUseCase.processDonation(
                persisted.id(), canonicalEvent(persisted)));
    }

    Instant now() {
        return Instant.now();
    }

    private DonationResult create(DonationReceived donation, String donorUserId) {
        return donationPort.save(new SaveDonationCommand(
                donation.ingestionKey(),
                normalizeType(donation.donationType()),
                donation.channelId(),
                donorUserId,
                normalize(donation.donatorNickname()),
                parseAmount(donation.payAmount()),
                donation.donationText(),
                now()
        ));
    }

    private DonationReceived canonicalEvent(DonationResult donation) {
        return new DonationReceived(
                donation.ingestionKey(),
                donation.donationType(),
                donation.recipientUserId(),
                donation.donorUserId(),
                donation.donorDisplayName(),
                Long.toString(donation.amount()),
                donation.message(),
                Map.of()
        );
    }

    private void publishPreparedRun(Optional<Long> runId) {
        runId.ifPresent(id -> eventPublisher.publishEvent(new RouletteRunPreparedEvent(id)));
    }

    private String normalizeType(String value) {
        return isBlank(value) ? "UNKNOWN" : value.trim();
    }

    private String normalize(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private long parseAmount(String amount) {
        if (isBlank(amount)) {
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
