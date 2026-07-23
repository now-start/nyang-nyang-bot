package org.nowstart.nyangnyangbot.application.port.in.roulette;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QueryRouletteResultUseCase {

    Page<RouletteRunSummaryResult> getRecentRuns(Pageable pageable);

    record RouletteRoundResult(
            Long id,
            Integer roundNo,
            String optionLabel,
            Boolean losing,
            String rewardType,
            String conversionMode,
            Long pointDelta,
            String status,
            String failureReason
    ) {
    }

    record RouletteRunSummaryResult(
            Long runId,
            String ingestionKey,
            String userId,
            String donorDisplayName,
            Long donationAmount,
            Integer roundCount,
            String status,
            Instant createdAt
    ) {
    }
}
