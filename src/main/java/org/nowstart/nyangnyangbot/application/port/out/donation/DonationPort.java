package org.nowstart.nyangnyangbot.application.port.out.donation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundResult;

public interface DonationPort {

    Optional<DonationResult> findByIngestionKey(String ingestionKey);

    DonationResult save(SaveDonationCommand command);

    record SaveDonationCommand(
            @NotBlank(message = "ingestionKey is required") String ingestionKey,
            @NotBlank(message = "donationType is required") String donationType,
            @NotBlank(message = "recipientUserId is required") String recipientUserId,
            String donorUserId,
            String donorDisplayName,
            @PositiveOrZero(message = "amount must not be negative") long amount,
            String message,
            @NotNull(message = "receivedAt is required") Instant receivedAt
    ) {
    }

    record DonationResult(
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive") Long id,
            @NotBlank(message = "ingestionKey is required") String ingestionKey,
            @NotBlank(message = "donationType is required") String donationType,
            @NotBlank(message = "recipientUserId is required") String recipientUserId,
            String donorUserId,
            String donorDisplayName,
            @PositiveOrZero(message = "amount must not be negative") long amount,
            String message
    ) {
    }
}
