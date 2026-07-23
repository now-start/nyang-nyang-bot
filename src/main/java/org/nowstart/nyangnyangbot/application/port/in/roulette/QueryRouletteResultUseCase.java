package org.nowstart.nyangnyangbot.application.port.in.roulette;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QueryRouletteResultUseCase {

    List<RouletteRoundResult> getRecentRounds(String userId, int limit);

    List<RouletteRunResult> getUserRuns(String userId);

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

    record RouletteRunResult(
            Long runId,
            String ingestionKey,
            String userId,
            String donorDisplayName,
            Long donationAmount,
            Integer roundCount,
            String status,
            Instant createdAt,
            List<RouletteRoundResult> rounds
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
