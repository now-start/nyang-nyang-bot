package org.nowstart.nyangnyangbot.application.port.in.roulette;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QueryRouletteResultUseCase {

    /** 최근 룰렛 실행과 각 실행의 회차 요약을 반환한다. */
    Page<RouletteRunSummaryResult> getRecentRuns(
            @NotNull(message = "pageable is required") Pageable pageable
    );

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
