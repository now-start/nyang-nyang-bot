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
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;

public interface OverlayDisplayPort {

    int MAX_DISPLAY_ROUNDS = 5;

    /** 멱등성 키마다 하나의 대기 중 표시 작업을 생성하며, 이미 존재하는 키는 무시한다. */
    void enqueue(
            Long rouletteRunId,
            String idempotencyKey,
            Instant expiresAt,
            Instant createdAt
    );

    /** 최근 표시 작업과 연결된 재표시 대기 작업을 생성하고 식별자를 반환한다. */
    Long replay(
            Long rouletteRunId,
            String idempotencyKey,
            Instant expiresAt,
            Instant createdAt
    );

    /** 만료 시각이 지난 대기 또는 표시 중 작업을 누락 상태로 변경한다. */
    void markExpiredMissed(Instant current);

    /** 다음 실행 가능한 작업을 원자적으로 선점하며, 가능한 작업이 없으면 빈 값을 반환한다. */
    @Valid
    Optional<DisplayJobResult> claimNext(Instant current, String claimToken, Instant claimExpiresAt);

    /** 전달된 토큰이 활성 선점을 소유한 경우에만 작업을 표시 완료로 변경한다. */
    void markDisplayed(Long displayJobId, String claimToken, Instant displayedAt);

    record DisplayJobResult(
            @NotNull(message = "id is required")
            @Positive(message = "id must be positive") Long id,
            String donorDisplayName,
            @NotBlank(message = "claimToken is required") String claimToken,
            @PositiveOrZero(message = "roundCount must not be negative") long roundCount,
            @NotNull(message = "rounds are required")
            @Size(max = MAX_DISPLAY_ROUNDS, message = "rounds must contain at most 5 entries")
            List<@Valid @NotNull(message = "display round is required") DisplayRoundResult> rounds
    ) {
    }

    record DisplayRoundResult(
            @NotNull(message = "id is required")
            @Positive(message = "id must be positive") Long id,
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
