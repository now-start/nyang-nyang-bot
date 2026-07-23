package org.nowstart.nyangnyangbot.application.service.roulette;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.DonationReceived;
import org.nowstart.nyangnyangbot.application.port.in.overlay.QueueOverlayDisplayUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ProcessRouletteDonationUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.RecoverRouletteRunsUseCase;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.ConfigResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.CreateRoundCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.CreateRunCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.OptionResult;
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
    public Optional<Long> processDonation(Long donationId, DonationReceived donation) {
        if (donationId == null || donation == null) {
            return Optional.empty();
        }
        if (isBlank(donation.donatorChannelId())) {
            return Optional.empty();
        }
        if (roulettePort.existsRun(donationId)) {
            return Optional.of(donationId);
        }
        ConfigResult config = roulettePort.findActiveConfigForUpdate().orElse(null);
        if (config == null) {
            return Optional.empty();
        }
        if (!roulettePolicy.containsTriggerToken(donation.donationText(), config.triggerToken())) {
            return Optional.empty();
        }
        long amount = roulettePolicy.parseDonationAmount(donation.payAmount());
        int roundCount;
        try {
            roundCount = roulettePolicy.calculateRoundCount(amount, config.pricePerRound());
        } catch (IllegalArgumentException tooManyRounds) {
            log.warn("action=roulette.rejected donationId={} reason={}", donationId, tooManyRounds.getMessage());
            return Optional.empty();
        }
        if (roundCount < 1) {
            return Optional.empty();
        }
        if (roundCount > roulettePolicy.highRoundThreshold(config)) {
            log.info("action=roulette.high_round donationId={} roundCount={}", donationId, roundCount);
        }

        List<OptionResult> options = roulettePort.findOptionsByConfigId(config.id());
        RouletteActivationValidation validation = roulettePolicy.validateActivation(config, options);
        if (!validation.activatable()) {
            return Optional.empty();
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
                return Optional.of(donationId);
            }
            throw conflict;
        }
        return Optional.of(run.id());
    }

    Instant now() {
        return Instant.now();
    }

    @Override
    public void recoverRun(Long runId) {
        if (runId == null || !roulettePort.existsRun(runId)) {
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

    private void resumeExistingRun(Long runId) {
        roulettePort.findRoundsByRunId(runId).forEach(round -> applyRound(round.id()));
        queueOverlayDisplayUseCase.enqueueRouletteRun(runId);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
