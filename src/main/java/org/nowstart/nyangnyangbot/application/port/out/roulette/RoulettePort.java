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
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundResult;
import org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteConfigStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoulettePort {

    ConfigResult createConfig(CreateConfigCommand command);

    OptionResult addOption(CreateOptionCommand command);

    Page<ConfigResult> findConfigs(Pageable pageable);

    Optional<ConfigResult> findConfigById(Long configId);

    List<OptionResult> findOptionsByConfigId(Long configId);

    Optional<ConfigResult> findActiveConfigForUpdate();

    ConfigResult activateConfig(Long configId, Instant activatedAt);

    ConfigResult archiveConfig(Long configId, Instant archivedAt);

    boolean existsRun(Long donationId);

    RunResult createReadyRun(CreateRunCommand command);

    Page<RunResult> findRecentRuns(Pageable pageable);

    List<RunRoundSummaryResult> summarizeRuns(List<Long> runIds);

    List<RoundResult> findRoundsByRunId(Long runId);

    List<Long> findRunIdsNeedingRecovery(long afterRunId, int limit);

    Long findMaxRunIdNeedingRecovery();

    Optional<RoundResult> findRoundByIdForUpdate(Long roundId);

    void markRoundApplied(Long roundId, Instant appliedAt);

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
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive") Long id,
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
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive") Long id,
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
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive") Long id,
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
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive") Long id,
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
