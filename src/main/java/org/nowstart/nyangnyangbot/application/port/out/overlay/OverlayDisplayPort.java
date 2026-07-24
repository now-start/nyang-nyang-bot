package org.nowstart.nyangnyangbot.application.port.out.overlay;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
            Long id,
            String donorDisplayName,
            String claimToken,
            long roundCount,
            List<DisplayRoundResult> rounds
    ) {
    }

    record DisplayRoundResult(
            Long id,
            Integer roundNo,
            String optionLabel,
            boolean losing,
            RewardType rewardType,
            ConversionMode conversionMode,
            Long pointDelta,
            RouletteRoundStatus status,
            String failureReason
    ) {
    }
}
