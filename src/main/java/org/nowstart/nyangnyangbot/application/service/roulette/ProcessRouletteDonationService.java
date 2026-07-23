package org.nowstart.nyangnyangbot.application.service.roulette;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.DonationReceived;
import org.nowstart.nyangnyangbot.application.port.in.overlay.QueueOverlayDisplayUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ProcessRouletteDonationUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.RecoverRouletteRunsUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteRoundResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.ConfigResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.CreateRoundCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.CreateRunCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.OptionResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RunResult;
import org.nowstart.nyangnyangbot.domain.roulette.RouletteActivationValidation;
import org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessRouletteDonationService
        implements ProcessRouletteDonationUseCase, RecoverRouletteRunsUseCase {

    private final RoulettePolicy roulettePolicy = new RoulettePolicy();
    private final RoulettePort roulettePort;
    private final RouletteRoundApplyService rouletteRoundApplyService;
    private final QueueOverlayDisplayUseCase queueOverlayDisplayUseCase;
    private final AtomicLong recoveryCursor = new AtomicLong(-1L);

    @Override
    @Transactional
    public RouletteRunResult processDonation(Long donationId, DonationReceived donation) {
        if (donationId == null || donation == null) {
            return RouletteRunResult.ignored("donation is required");
        }
        if (isBlank(donation.donatorChannelId())) {
            return RouletteRunResult.ignored("identified donor is required");
        }
        if (roulettePort.existsRun(donationId)) {
            return preparedExistingRun(donationId);
        }
        ConfigResult config = roulettePort.findActiveConfigForUpdate().orElse(null);
        if (config == null) {
            return RouletteRunResult.ignored("active roulette config not found");
        }
        if (!roulettePolicy.containsTriggerToken(donation.donationText(), config.triggerToken())) {
            return RouletteRunResult.ignored("roulette trigger token not found");
        }
        long amount = roulettePolicy.parseDonationAmount(donation.payAmount());
        int roundCount;
        try {
            roundCount = roulettePolicy.calculateRoundCount(amount, config.pricePerRound());
        } catch (IllegalArgumentException tooManyRounds) {
            log.warn("action=roulette.rejected donationId={} reason={}", donationId, tooManyRounds.getMessage());
            return RouletteRunResult.ignored(tooManyRounds.getMessage());
        }
        if (roundCount < 1) {
            return RouletteRunResult.ignored("donation amount is less than roulette price");
        }
        if (roundCount > roulettePolicy.highRoundThreshold(config)) {
            log.info("action=roulette.high_round donationId={} roundCount={}", donationId, roundCount);
        }

        List<OptionResult> options = roulettePort.findOptionsByConfigId(config.id());
        RouletteActivationValidation validation = roulettePolicy.validateActivation(config, options);
        if (!validation.activatable()) {
            return RouletteRunResult.ignored("active roulette config is invalid");
        }
        Instant createdAt = now();
        RunResult run;
        try {
            run = roulettePort.createReadyRun(new CreateRunCommand(
                    donationId,
                    config.id(),
                    createdAt,
                    drawRounds(roundCount, options)
            ));
        } catch (IllegalStateException conflict) {
            if (roulettePort.existsRun(donationId)) {
                return preparedExistingRun(donationId);
            }
            throw conflict;
        }
        List<RouletteRoundResult> rounds = roulettePort.findRoundsByRunId(run.id())
                .stream()
                .map(this::roundResult)
                .toList();
        return RouletteRunResult.ready(run.id(), rounds);
    }

    Instant now() {
        return Instant.now();
    }

    @Override
    public void recoverRun(Long runId) {
        if (runId == null || roulettePort.findRunById(runId).isEmpty()) {
            return;
        }
        resumeExistingRun(runId);
    }

    @Override
    public int recoverPendingRuns(int limit) {
        int recovered = 0;
        List<Long> runIds = roulettePort.findRunIdsNeedingRecovery(recoveryStart(), limit);
        for (Long runId : runIds) {
            try {
                resumeExistingRun(runId);
                recovered++;
            } catch (RuntimeException retryableFailure) {
                log.warn("action=roulette.recovery result=retry_later runId={}", runId, retryableFailure);
            }
        }
        if (!runIds.isEmpty()) {
            recoveryCursor.set(runIds.getLast());
        }
        return recovered;
    }

    private long recoveryStart() {
        long current = recoveryCursor.get();
        if (current >= 0) {
            return current;
        }
        Long maximum = roulettePort.findMaxRunIdNeedingRecovery();
        long seed = maximum == null || maximum <= 1
                ? 0L
                : ThreadLocalRandom.current().nextLong(maximum);
        recoveryCursor.compareAndSet(-1L, seed);
        return recoveryCursor.get();
    }

    private List<CreateRoundCommand> drawRounds(int roundCount, List<OptionResult> options) {
        List<CreateRoundCommand> rounds = new ArrayList<>(roundCount);
        for (int roundNo = 1; roundNo <= roundCount; roundNo++) {
            int ticket = roulettePolicy.nextTicket(RoulettePolicy.TOTAL_PROBABILITY);
            OptionResult selected = roulettePolicy.selectOption(options, ticket);
            rounds.add(new CreateRoundCommand(selected.id(), roundNo, ticket));
        }
        return rounds;
    }

    private RouletteRoundResult roundResult(RoundResult round) {
        return new RouletteRoundResult(
                round.id(),
                round.roundNo(),
                round.optionLabel(),
                round.losing(),
                round.rewardType().name(),
                round.conversionMode().name(),
                round.pointDelta(),
                round.status().name(),
                round.failureReason()
        );
    }

    private void applyRound(Long roundId) {
        try {
            rouletteRoundApplyService.applyRound(roundId);
        } catch (RuntimeException failure) {
            if (isRetryable(failure)) {
                throw failure;
            }
            log.warn("action=roulette.reward result=failed roundId={}", roundId, failure);
            rouletteRoundApplyService.failRound(roundId, failure.getMessage());
        }
    }

    private boolean isRetryable(RuntimeException failure) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof TransientDataAccessException
                    || cause instanceof RecoverableDataAccessException
                    || cause instanceof DataAccessResourceFailureException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private RouletteRunResult resumeExistingRun(Long runId) {
        roulettePort.findRoundsByRunId(runId).forEach(round -> applyRound(round.id()));
        queueOverlayDisplayUseCase.enqueueRouletteRun(runId);
        List<RouletteRoundResult> rounds = roulettePort.findRoundsByRunId(runId).stream()
                .map(this::roundResult)
                .toList();
        return RouletteRunResult.duplicate(runId, rounds);
    }

    private RouletteRunResult preparedExistingRun(Long runId) {
        List<RouletteRoundResult> rounds = roulettePort.findRoundsByRunId(runId).stream()
                .map(this::roundResult)
                .toList();
        return RouletteRunResult.duplicate(runId, rounds);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
