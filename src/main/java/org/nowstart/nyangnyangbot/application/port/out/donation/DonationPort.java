package org.nowstart.nyangnyangbot.application.port.out.donation;

import java.time.Instant;
import java.util.Optional;

public interface DonationPort {

    Optional<DonationResult> findByIngestionKey(String ingestionKey);

    DonationResult save(SaveDonationCommand command);

    record SaveDonationCommand(
            String ingestionKey,
            String donationType,
            String recipientUserId,
            String donorUserId,
            String donorDisplayName,
            long amount,
            String message,
            Instant receivedAt
    ) {
    }

    record DonationResult(
            Long id,
            String ingestionKey,
            String donationType,
            String recipientUserId,
            String donorUserId,
            String donorDisplayName,
            long amount,
            String message
    ) {
    }
}
