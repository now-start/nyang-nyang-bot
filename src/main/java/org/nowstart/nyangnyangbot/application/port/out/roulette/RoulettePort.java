package org.nowstart.nyangnyangbot.application.port.out.roulette;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteConfigStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoulettePort {

    /** 초안 상태의 룰렛 설정을 저장한다. */
    @Valid
    ConfigResult createConfig(
            @Valid @NotNull(message = "roulette config command is required") CreateConfigCommand command
    );

    /** 초안 상태의 룰렛 설정에 선택지를 저장한다. */
    @Valid
    OptionResult addOption(
            @Valid @NotNull(message = "roulette option command is required") CreateOptionCommand command
    );

    /** 룰렛 설정을 최신순으로 반환한다. */
    Page<@Valid ConfigResult> findConfigs(Pageable pageable);

    /** 식별자로 룰렛 설정을 조회한다. */
    Optional<@Valid ConfigResult> findConfigById(Long configId);

    /** 룰렛 설정의 선택지를 표시 순서로 반환한다. */
    List<@Valid OptionResult> findOptionsByConfigId(Long configId);

    /** 활성 룰렛 설정을 조회하고 현재 트랜잭션 동안 쓰기 잠금을 유지한다. */
    Optional<@Valid ConfigResult> findActiveConfigForUpdate();

    /** 유효한 초안 설정을 활성화하고 다른 활성 설정을 보관 상태로 변경한다. */
    @Valid
    ConfigResult activateConfig(Long configId, Instant activatedAt);

    /** 지정한 시각에 해당 룰렛 설정을 보관 상태로 변경한다. */
    @Valid
    ConfigResult archiveConfig(Long configId, Instant archivedAt);

    /** 후원 식별자에 해당하는 룰렛 실행이 이미 존재하는지 반환한다. */
    boolean existsRun(Long donationId);

    /** 대상 후원의 준비 완료 실행과 모든 회차를 원자적으로 생성한다. */
    @Valid
    RunResult createReadyRun(
            @Valid @NotNull(message = "roulette run command is required") CreateRunCommand command
    );

    /** 룰렛 실행을 최신순으로 반환한다. */
    Page<@Valid RunResult> findRecentRuns(Pageable pageable);

    /** 전달된 실행 식별자별 회차 개수 요약을 반환한다. */
    List<@Valid RunRoundSummaryResult> summarizeRuns(List<Long> runIds);

    /** 룰렛 실행의 회차를 회차 번호 오름차순으로 반환한다. */
    List<@Valid RoundResult> findRoundsByRunId(Long runId);

    /** {@code afterRunId}를 기준으로 순환 정렬한 복구 대상 실행 식별자를 반환한다. */
    List<Long> findRunIdsNeedingRecovery(long afterRunId, int limit);

    /** 복구 대상 중 가장 큰 실행 식별자를 반환하며, 대상이 없으면 {@code null}을 반환한다. */
    Long findMaxRunIdNeedingRecovery();

    /** 식별자로 회차를 조회하고 현재 트랜잭션 동안 쓰기 잠금을 유지한다. */
    Optional<@Valid RoundResult> findRoundByIdForUpdate(Long roundId);

    /** 잠근 회차를 적용 완료 상태로 변경한다. */
    void markRoundApplied(Long roundId, Instant appliedAt);

    /** 잠근 회차를 전달된 사유와 함께 실패 상태로 변경한다. */
    void markRoundFailed(Long roundId, String failureReason, Instant failedAt);

    record CreateConfigCommand(
            @NotBlank(message = "title is required") String title,
            @NotBlank(message = "triggerToken is required") String triggerToken,
            @NotNull(message = "pricePerRound is required")
            @Positive(message = "pricePerRound must be positive") Long pricePerRound,
            @NotNull(message = "highRoundThreshold is required")
            @Positive(message = "highRoundThreshold must be positive") Integer highRoundThreshold,
            @NotNull(message = "createdAt is required") Instant createdAt
    ) {
    }

    record CreateOptionCommand(
            @NotNull(message = "configId is required")
            @Positive(message = "configId must be positive") Long configId,
            @NotBlank(message = "label is required") String label,
            @NotNull(message = "probabilityBasisPoints is required")
            @PositiveOrZero(message = "probabilityBasisPoints must not be negative")
            @Max(value = RoulettePolicy.TOTAL_PROBABILITY,
                    message = "probabilityBasisPoints must not exceed 10000") Integer probabilityBasisPoints,
            boolean losing,
            @NotNull(message = "rewardType is required") RewardType rewardType,
            @NotNull(message = "conversionMode is required") ConversionMode conversionMode,
            Long pointDelta,
            @NotNull(message = "displayOrder is required")
            @PositiveOrZero(message = "displayOrder must not be negative") Integer displayOrder,
            @NotNull(message = "createdAt is required") Instant createdAt
    ) {
    }

    record ConfigResult(
            @NotNull(message = "id is required")
            @Positive(message = "id must be positive") Long id,
            @NotBlank(message = "title is required") String title,
            @NotBlank(message = "triggerToken is required") String triggerToken,
            @NotNull(message = "pricePerRound is required")
            @Positive(message = "pricePerRound must be positive") Long pricePerRound,
            @NotNull(message = "highRoundThreshold is required")
            @Positive(message = "highRoundThreshold must be positive") Integer highRoundThreshold,
            @NotNull(message = "status is required") RouletteConfigStatus status,
            @NotNull(message = "createdAt is required") Instant createdAt,
            @NotNull(message = "updatedAt is required") Instant updatedAt
    ) implements RoulettePolicy.ConfigCandidate {
    }

    record OptionResult(
            @NotNull(message = "id is required")
            @Positive(message = "id must be positive") Long id,
            @NotBlank(message = "label is required") String label,
            @NotNull(message = "probabilityBasisPoints is required")
            @PositiveOrZero(message = "probabilityBasisPoints must not be negative")
            @Max(value = RoulettePolicy.TOTAL_PROBABILITY,
                    message = "probabilityBasisPoints must not exceed 10000") Integer probabilityBasisPoints,
            boolean losing,
            @NotNull(message = "rewardType is required") RewardType rewardType,
            @NotNull(message = "conversionMode is required") ConversionMode conversionMode,
            Long pointDelta,
            @NotNull(message = "displayOrder is required")
            @PositiveOrZero(message = "displayOrder must not be negative") Integer displayOrder
    ) implements RoulettePolicy.OptionCandidate {
    }

    record CreateRunCommand(
            @NotNull(message = "donationId is required")
            @Positive(message = "donationId must be positive") Long donationId,
            @NotNull(message = "configId is required")
            @Positive(message = "configId must be positive") Long configId,
            @NotNull(message = "createdAt is required") Instant createdAt,
            @NotEmpty(message = "rounds are required")
            List<@Valid @NotNull(message = "round is required") CreateRoundCommand> rounds
    ) {
        @AssertTrue(message = "round numbers must be contiguous from 1")
        public boolean hasContiguousRoundNumbers() {
            if (rounds == null) {
                return true;
            }
            for (int index = 0; index < rounds.size(); index++) {
                CreateRoundCommand round = rounds.get(index);
                if (round == null || round.roundNo() == null) {
                    continue;
                }
                if (round.roundNo() != index + 1) {
                    return false;
                }
            }
            return true;
        }
    }

    record CreateRoundCommand(
            @NotNull(message = "optionId is required")
            @Positive(message = "optionId must be positive") Long optionId,
            @NotNull(message = "roundNo is required")
            @Positive(message = "roundNo must be positive") Integer roundNo,
            @NotNull(message = "ticket is required")
            @Min(value = 1, message = "ticket must be at least 1")
            @Max(value = RoulettePolicy.TOTAL_PROBABILITY, message = "ticket must not exceed 10000") Integer ticket
    ) {
    }

    record RunResult(
            @NotNull(message = "id is required")
            @Positive(message = "id must be positive") Long id,
            @NotBlank(message = "ingestionKey is required") String ingestionKey,
            @NotBlank(message = "userId is required") String userId,
            String donorDisplayName,
            @NotNull(message = "donationAmount is required")
            @PositiveOrZero(message = "donationAmount must not be negative") Long donationAmount,
            @NotNull(message = "createdAt is required") Instant createdAt
    ) {
    }

    record RunRoundSummaryResult(
            @NotNull(message = "runId is required")
            @Positive(message = "runId must be positive") Long runId,
            @PositiveOrZero(message = "roundCount must not be negative") long roundCount,
            @PositiveOrZero(message = "appliedCount must not be negative") long appliedCount,
            @PositiveOrZero(message = "failedCount must not be negative") long failedCount
    ) {
        public static RunRoundSummaryResult empty(Long runId) {
            return new RunRoundSummaryResult(runId, 0, 0, 0);
        }
    }

    record RoundResult(
            @NotNull(message = "id is required")
            @Positive(message = "id must be positive") Long id,
            @NotBlank(message = "ingestionKey is required") String ingestionKey,
            @NotBlank(message = "userId is required") String userId,
            String donorDisplayName,
            @NotNull(message = "roundNo is required")
            @Positive(message = "roundNo must be positive") Integer roundNo,
            @NotBlank(message = "optionLabel is required") String optionLabel,
            boolean losing,
            @NotNull(message = "rewardType is required") RewardType rewardType,
            @NotNull(message = "conversionMode is required") ConversionMode conversionMode,
            Long pointDelta,
            @NotNull(message = "status is required") RouletteRoundStatus status
    ) {
    }
}
