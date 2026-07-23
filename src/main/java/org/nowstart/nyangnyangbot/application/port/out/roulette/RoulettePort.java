package org.nowstart.nyangnyangbot.application.port.out.roulette;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteConfigStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRunStatus;
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

    Optional<RunResult> findRunById(Long runId);

    List<RunResult> findRunsByUserId(String userId);

    Page<RunResult> findRecentRuns(Pageable pageable);

    List<RunRoundSummaryResult> summarizeRuns(List<Long> runIds);

    List<RoundResult> findRoundsByRunId(Long runId);

    List<RoundResult> findRoundsByUserId(String userId);

    List<Long> findRunIdsNeedingRecovery(long afterRunId, int limit);

    Long findMaxRunIdNeedingRecovery();

    Optional<RoundResult> findRoundById(Long roundId);

    Optional<RoundResult> findRoundByIdForUpdate(Long roundId);

    void markRoundApplied(Long roundId, Instant appliedAt);

    void markRoundFailed(Long roundId, String failureReason, Instant failedAt);

    record CreateConfigCommand(
            String title,
            String triggerToken,
            Long pricePerRound,
            Integer highRoundThreshold,
            Instant createdAt
    ) {
    }

    record CreateOptionCommand(
            Long configId,
            String label,
            Integer probabilityBasisPoints,
            boolean losing,
            RewardType rewardType,
            ConversionMode conversionMode,
            Long pointDelta,
            Integer displayOrder,
            Instant createdAt
    ) {
    }

    record ConfigResult(
            Long id,
            String title,
            String triggerToken,
            Long pricePerRound,
            Integer highRoundThreshold,
            RouletteConfigStatus status,
            Instant createdAt,
            Instant updatedAt
    ) implements RoulettePolicy.ConfigCandidate {
    }

    record OptionResult(
            Long id,
            Long configId,
            String label,
            Integer probabilityBasisPoints,
            boolean losing,
            RewardType rewardType,
            ConversionMode conversionMode,
            Long pointDelta,
            Integer displayOrder,
            Instant createdAt
    ) implements RoulettePolicy.OptionCandidate {
    }

    record CreateRunCommand(
            Long donationId,
            Long configId,
            Instant createdAt,
            List<CreateRoundCommand> rounds
    ) {
    }

    record CreateRoundCommand(
            Long optionId,
            Integer roundNo,
            Integer ticket
    ) {
    }

    record RunResult(
            Long id,
            Long configId,
            String ingestionKey,
            String userId,
            String donorDisplayName,
            Long donationAmount,
            RouletteRunStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    record RunRoundSummaryResult(
            Long runId,
            long roundCount,
            long appliedCount,
            long failedCount
    ) {
    }

    record RoundResult(
            Long id,
            Long runId,
            Long configId,
            String ingestionKey,
            String userId,
            String donorDisplayName,
            Long optionId,
            Integer roundNo,
            String optionLabel,
            boolean losing,
            RewardType rewardType,
            ConversionMode conversionMode,
            Long pointDelta,
            RouletteRoundStatus status,
            String failureReason,
            Integer ticket,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
