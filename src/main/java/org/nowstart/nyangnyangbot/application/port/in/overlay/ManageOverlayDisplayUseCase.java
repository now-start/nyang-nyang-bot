package org.nowstart.nyangnyangbot.application.port.in.overlay;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteRoundResult;

public interface ManageOverlayDisplayUseCase {

    /** 지정한 룰렛 실행을 다시 표시할 작업을 새로 등록한다. */
    void replayRouletteRun(
            @NotNull(message = "rouletteRunId is required")
            @Positive(message = "rouletteRunId must be positive") Long rouletteRunId
    );

    /** 인증된 오버레이가 다음 표시 작업을 선점하며, 가능한 작업이 없으면 빈 값을 반환한다. */
    Optional<OverlayDisplayResult> claimNextJob(
            @NotBlank(message = "authorizationHeader is required") String authorizationHeader
    );

    /** 선점 토큰과 인증이 유효하면 해당 표시 작업을 표시 완료로 변경한다. */
    void markDisplayed(
            @NotNull(message = "displayJobId is required")
            @Positive(message = "displayJobId must be positive") Long displayJobId,
            @NotBlank(message = "claimToken is required") String claimToken,
            @NotBlank(message = "authorizationHeader is required") String authorizationHeader
    );

    record OverlayDisplayResult(
            Long displayJobId,
            String donorDisplayName,
            String claimToken,
            Integer roundCount,
            List<RouletteRoundResult> rounds
    ) {
    }
}
