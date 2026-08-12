package org.nowstart.nyangnyangbot.application.port.out.donation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.Optional;

public interface DonationPort {

    /** 멱등성 키인 수집 키로 이전에 저장된 후원을 조회한다. */
    Optional<@Valid DonationResult> findByIngestionKey(String ingestionKey);

    /** 후원을 저장하고 저장된 결과를 반환한다. */
    @Valid
    DonationResult save(
            @Valid @NotNull(message = "donation command is required") SaveDonationCommand command
    );

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
            @NotNull(message = "id is required")
            @Positive(message = "id must be positive") Long id,
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
