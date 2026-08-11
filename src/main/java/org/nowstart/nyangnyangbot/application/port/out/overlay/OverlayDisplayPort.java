package org.nowstart.nyangnyangbot.application.port.out.overlay;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundResult;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;

public interface OverlayDisplayPort {

    int MAX_DISPLAY_ROUNDS = 5;

    void enqueue(
            Long rouletteRunId,
            String idempotencyKey,
            Instant expiresAt,
            Instant createdAt
    );

    Long replay(
            Long rouletteRunId,
            String idempotencyKey,
            Instant expiresAt,
            Instant createdAt
    );

    void markExpiredMissed(Instant current);

    Optional<DisplayJobResult> claimNext(Instant current, String claimToken, Instant claimExpiresAt);

    void markDisplayed(Long displayJobId, String claimToken, Instant displayedAt);

    record DisplayJobResult(
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive") Long id,
            String donorDisplayName,
            @NotBlank(message = "claimToken is required") String claimToken,
            @PositiveOrZero(message = "roundCount must not be negative") long roundCount,
            @NotNull(message = "rounds are required")
            @Size(max = MAX_DISPLAY_ROUNDS, message = "rounds must contain at most 5 entries")
            List<@Valid @NotNull(message = "display round is required") DisplayRoundResult> rounds
    ) {
    }

    record DisplayRoundResult(
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive") Long id,
            @NotNull(message = "roundNo is required")
            @Positive(message = "roundNo must be positive") Integer roundNo,
            @NotBlank(message = "optionLabel is required") String optionLabel,
            boolean losing,
            @NotNull(message = "rewardType is required") RewardType rewardType,
            @NotNull(message = "conversionMode is required") ConversionMode conversionMode,
            Long pointDelta,
            @NotNull(message = "status is required") RouletteRoundStatus status,
            String failureReason
    ) {
    }
}
