package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.donation.entity.Donation;
import org.nowstart.nyangnyangbot.adapter.out.persistence.donation.repository.DonationRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteConfig;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteOption;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteRound;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteRun;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteConfigRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteOptionRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRoundRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRunRepository;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RecentRouletteResultQueryPort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.domain.type.RouletteConfigStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRunStatus;
import org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RoulettePersistenceAdapter implements RoulettePort, RecentRouletteResultQueryPort {

    private static final int RECENT_ROUND_LIMIT = 5;

    private final RouletteConfigRepository rouletteConfigRepository;
    private final RouletteOptionRepository rouletteOptionRepository;
    private final RouletteRunRepository rouletteRunRepository;
    private final RouletteRoundRepository rouletteRoundRepository;
    private final DonationRepository donationRepository;
    private final OutboundContractValidator contractValidator;
    private final RoulettePolicy roulettePolicy = new RoulettePolicy();

    @Override
    @Transactional
    public ConfigResult createConfig(CreateConfigCommand command) {
        RouletteConfig saved = rouletteConfigRepository.save(RouletteConfig.builder()
                .title(command.title())
                .triggerToken(command.triggerToken())
                .pricePerRound(command.pricePerRound())
                .highRoundThreshold(command.highRoundThreshold())
                .status(RouletteConfigStatus.DRAFT)
                .createdAt(command.createdAt())
                .updatedAt(command.createdAt())
                .build());
        return configResult(saved);
    }

    @Override
    @Transactional
    public OptionResult addOption(CreateOptionCommand command) {
        RouletteConfig config = rouletteConfigRepository.findByIdForUpdate(command.configId())
                .orElseThrow(() -> new IllegalArgumentException("roulette config not found"));
        if (config.getStatus() != RouletteConfigStatus.DRAFT) {
            throw new IllegalStateException("roulette options can only be added to DRAFT config");
        }
        RouletteOption saved = rouletteOptionRepository.save(RouletteOption.builder()
                .rouletteConfig(config)
                .label(command.label())
                .probabilityBasisPoints(command.probabilityBasisPoints())
                .losing(command.losing())
                .rewardType(command.rewardType())
                .conversionMode(command.conversionMode())
                .pointDelta(command.pointDelta())
                .displayOrder(command.displayOrder())
                .createdAt(command.createdAt())
                .build());
        return optionResult(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConfigResult> findConfigs(Pageable pageable) {
        return rouletteConfigRepository.findAllByOrderByIdDesc(pageable).map(this::configResult);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConfigResult> findConfigById(Long configId) {
        return rouletteConfigRepository.findById(configId).map(this::configResult);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OptionResult> findOptionsByConfigId(Long configId) {
        return rouletteOptionRepository.findByRouletteConfig_IdOrderByDisplayOrderAscIdAsc(configId)
                .stream()
                .map(this::optionResult)
                .toList();
    }

    @Override
    @Transactional
    public Optional<ConfigResult> findActiveConfigForUpdate() {
        return rouletteConfigRepository.findByStatusForUpdate(RouletteConfigStatus.ACTIVE).stream()
                .findFirst()
                .map(this::configResult);
    }

    @Override
    @Transactional
    public ConfigResult activateConfig(Long configId, java.time.Instant activatedAt) {
        RouletteConfig target = rouletteConfigRepository.findByIdForUpdate(configId)
                .orElseThrow(() -> new IllegalArgumentException("roulette config not found"));
        if (target.getStatus() == RouletteConfigStatus.ACTIVE) {
            return configResult(target);
        }
        if (target.getStatus() != RouletteConfigStatus.DRAFT) {
            throw new IllegalStateException("only DRAFT roulette config can be activated");
        }
        var options = rouletteOptionRepository.findByRouletteConfig_IdOrderByDisplayOrderAscIdAsc(configId);
        var validation = roulettePolicy.validateActivation(configResult(target), options.stream()
                .map(this::optionResult)
                .toList());
        if (!validation.activatable()) {
            throw new IllegalStateException(String.join(", ", validation.reasons()));
        }
        rouletteConfigRepository.findByStatusForUpdate(RouletteConfigStatus.ACTIVE)
                .stream()
                .filter(active -> !active.getId().equals(configId))
                .forEach(active -> active.archive(activatedAt));
        rouletteConfigRepository.flush();
        target.activate(activatedAt);
        return configResult(rouletteConfigRepository.saveAndFlush(target));
    }

    @Override
    @Transactional
    public ConfigResult archiveConfig(Long configId, java.time.Instant archivedAt) {
        RouletteConfig config = rouletteConfigRepository.findByIdForUpdate(configId)
                .orElseThrow(() -> new IllegalArgumentException("roulette config not found"));
        config.archive(archivedAt);
        return configResult(config);
    }

    @Override
    public boolean existsRun(Long donationId) {
        return rouletteRunRepository.existsById(donationId);
    }

    @Override
    @Transactional
    public RunResult createReadyRun(CreateRunCommand command) {
        validateRoundCommands(command.rounds());
        if (rouletteRunRepository.existsById(command.donationId())) {
            throw new IllegalStateException("roulette run already exists");
        }
        Donation donation = donationRepository.findById(command.donationId())
                .orElseThrow(() -> new IllegalArgumentException("donation not found"));
        RouletteConfig config = rouletteConfigRepository.findByIdForUpdate(command.configId())
                .orElseThrow(() -> new IllegalArgumentException("roulette config not found"));
        if (rouletteRunRepository.existsById(command.donationId())) {
            throw new IllegalStateException("roulette run already exists");
        }
        if (config.getStatus() != RouletteConfigStatus.ACTIVE) {
            throw new IllegalStateException("roulette config is not ACTIVE");
        }

        Map<Long, RouletteOption> options = new HashMap<>();
        rouletteOptionRepository.findByRouletteConfig_IdOrderByDisplayOrderAscIdAsc(config.getId())
                .forEach(option -> options.put(option.getId(), option));
        RouletteRun run = rouletteRunRepository.save(RouletteRun.builder()
                .donationId(donation.getId())
                .donation(donation)
                .rouletteConfig(config)
                .status(RouletteRunStatus.BUILDING)
                .createdAt(command.createdAt())
                .updatedAt(command.createdAt())
                .build());
        List<RouletteRound> rounds = command.rounds().stream()
                .map(round -> newRound(run, config, requiredOption(options, round.optionId()), round, command))
                .toList();
        rouletteRoundRepository.saveAll(rounds);
        rouletteRoundRepository.flush();
        run.markReady(command.createdAt());
        rouletteRunRepository.flush();
        return runResult(run);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RunResult> findRunById(Long runId) {
        return rouletteRunRepository.findById(runId).map(this::runResult);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RunResult> findRecentRuns(Pageable pageable) {
        return rouletteRunRepository.findAllByOrderByCreatedAtDescDonationIdDesc(pageable).map(this::runResult);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RunRoundSummaryResult> summarizeRuns(List<Long> runIds) {
        if (runIds == null || runIds.isEmpty()) {
            return List.of();
        }
        return rouletteRoundRepository.summarizeRuns(runIds).stream()
                .map(summary -> new RunRoundSummaryResult(
                        summary.getRunId(),
                        summary.getRoundCount(),
                        summary.getAppliedCount(),
                        summary.getFailedCount()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoundResult> findRoundsByRunId(Long runId) {
        return rouletteRoundRepository.findByRouletteRun_DonationIdOrderByRoundNoAsc(runId)
                .stream()
                .map(this::roundResult)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findRunIdsNeedingRecovery(long afterRunId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return rouletteRunRepository.findRunIdsNeedingRecovery(afterRunId, safeLimit);
    }

    @Override
    @Transactional(readOnly = true)
    public Long findMaxRunIdNeedingRecovery() {
        return rouletteRunRepository.findMaxRunIdNeedingRecovery();
    }

    @Override
    @Transactional
    public Optional<RoundResult> findRoundByIdForUpdate(Long roundId) {
        return rouletteRoundRepository.findByIdForUpdate(roundId).map(this::roundResult);
    }

    @Override
    @Transactional
    public void markRoundApplied(Long roundId, java.time.Instant appliedAt) {
        RouletteRound round = rouletteRoundRepository.findByIdForUpdate(roundId)
                .orElseThrow(() -> new IllegalArgumentException("roulette round not found"));
        round.markApplied(appliedAt);
    }

    @Override
    @Transactional
    public void markRoundFailed(Long roundId, String failureReason, java.time.Instant failedAt) {
        RouletteRound round = rouletteRoundRepository.findByIdForUpdate(roundId)
                .orElseThrow(() -> new IllegalArgumentException("roulette round not found"));
        round.markFailed(failureReason, failedAt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentRound> findRecentRoundsByUserId(String userId) {
        return rouletteRoundRepository.findRecentByUserId(userId, PageRequest.of(0, RECENT_ROUND_LIMIT)).stream()
                .map(round -> new RecentRound(round.getRoundNo(), round.getItemLabel()))
                .toList();
    }

    private RouletteRound newRound(
            RouletteRun run,
            RouletteConfig config,
            RouletteOption option,
            CreateRoundCommand round,
            CreateRunCommand command
    ) {
        return RouletteRound.builder()
                .rouletteRun(run)
                .rouletteConfigId(config.getId())
                .rouletteOption(option)
                .roundNo(round.roundNo())
                .ticket(round.ticket())
                .status(RouletteRoundStatus.CONFIRMED)
                .createdAt(command.createdAt())
                .updatedAt(command.createdAt())
                .build();
    }

    private RouletteOption requiredOption(Map<Long, RouletteOption> options, Long optionId) {
        RouletteOption option = options.get(optionId);
        if (option == null) {
            throw new IllegalArgumentException("roulette option does not belong to config");
        }
        return option;
    }

    private void validateRoundCommands(List<CreateRoundCommand> rounds) {
        if (rounds == null || rounds.isEmpty()) {
            throw new IllegalArgumentException("roulette run requires at least one round");
        }
        for (int index = 0; index < rounds.size(); index++) {
            CreateRoundCommand round = rounds.get(index);
            if (round == null || round.roundNo() == null || round.roundNo() != index + 1) {
                throw new IllegalArgumentException("roulette round numbers must be contiguous from 1");
            }
            if (round.ticket() == null || round.ticket() < 1 || round.ticket() > 10_000) {
                throw new IllegalArgumentException("roulette round ticket must be between 1 and 10000");
            }
        }
    }

    private ConfigResult configResult(RouletteConfig config) {
        return contractValidator.persistenceResult("roulette.config", new ConfigResult(
                config.getId(),
                config.getTitle(),
                config.getTriggerToken(),
                config.getPricePerRound(),
                config.getHighRoundThreshold(),
                config.getStatus(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        ));
    }

    private OptionResult optionResult(RouletteOption option) {
        return contractValidator.persistenceResult("roulette.option", new OptionResult(
                option.getId(),
                option.getRouletteConfig().getId(),
                option.getLabel(),
                option.getProbabilityBasisPoints(),
                option.isLosing(),
                option.getRewardType(),
                option.getConversionMode(),
                option.getPointDelta(),
                option.getDisplayOrder(),
                option.getCreatedAt()
        ));
    }

    private RunResult runResult(RouletteRun run) {
        Donation donation = run.getDonation();
        return contractValidator.persistenceResult("roulette.run", new RunResult(
                run.getDonationId(),
                run.getRouletteConfig().getId(),
                donation.getIngestionKey(),
                donation.getDonorUserAccount() == null ? null : donation.getDonorUserAccount().getUserId(),
                donation.getDonorDisplayName(),
                donation.getAmount(),
                run.getStatus(),
                run.getCreatedAt(),
                run.getUpdatedAt()
        ));
    }

    private RoundResult roundResult(RouletteRound round) {
        RouletteRun run = round.getRouletteRun();
        Donation donation = run.getDonation();
        RouletteOption option = round.getRouletteOption();
        return contractValidator.persistenceResult("roulette.round", new RoundResult(
                round.getId(),
                run.getDonationId(),
                round.getRouletteConfigId(),
                donation.getIngestionKey(),
                donation.getDonorUserAccount() == null ? null : donation.getDonorUserAccount().getUserId(),
                donation.getDonorDisplayName(),
                option.getId(),
                round.getRoundNo(),
                option.getLabel(),
                option.isLosing(),
                option.getRewardType(),
                option.getConversionMode(),
                option.getPointDelta(),
                round.getStatus(),
                round.getFailureReason(),
                round.getTicket(),
                round.getCreatedAt(),
                round.getUpdatedAt()
        ));
    }
}
