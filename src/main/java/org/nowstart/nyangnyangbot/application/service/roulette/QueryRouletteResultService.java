package org.nowstart.nyangnyangbot.application.service.roulette;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RunResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RunRoundSummaryResult;
import org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueryRouletteResultService implements QueryRouletteResultUseCase {

    private final RoulettePolicy roulettePolicy = new RoulettePolicy();
    private final RoulettePort roulettePort;

    @Override
    public List<RouletteRoundResult> getRecentRounds(String userId, int limit) {
        requireUserId(userId);
        return roulettePort.findRoundsByUserId(userId).stream()
                .limit(roulettePolicy.safeRecentRoundLimit(limit))
                .map(this::roundResult)
                .toList();
    }

    @Override
    public List<RouletteRunResult> getUserRuns(String userId) {
        requireUserId(userId);
        return roulettePort.findRunsByUserId(userId).stream().map(this::runResult).toList();
    }

    @Override
    public Page<RouletteRunSummaryResult> getRecentRuns(Pageable pageable) {
        Page<RunResult> runs = roulettePort.findRecentRuns(pageable);
        Map<Long, RunRoundSummaryResult> summaries = roulettePort.summarizeRuns(
                        runs.stream().map(RunResult::id).toList()
                ).stream()
                .collect(Collectors.toMap(RunRoundSummaryResult::runId, Function.identity()));
        return runs.map(run -> runSummaryResult(run, summaries.get(run.id())));
    }

    private RouletteRunResult runResult(RunResult run) {
        List<RouletteRoundResult> rounds = roulettePort.findRoundsByRunId(run.id()).stream()
                .map(this::roundResult)
                .toList();
        return new RouletteRunResult(
                run.id(),
                run.ingestionKey(),
                run.userId(),
                run.donorDisplayName(),
                run.donationAmount(),
                rounds.size(),
                roulettePolicy.processingStatus(rounds.stream()
                        .map(round -> org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus.valueOf(round.status()))
                        .toList()).name(),
                run.createdAt(),
                rounds
        );
    }

    private RouletteRunSummaryResult runSummaryResult(RunResult run, RunRoundSummaryResult summary) {
        long roundCount = summary == null ? 0 : summary.roundCount();
        long appliedCount = summary == null ? 0 : summary.appliedCount();
        long failedCount = summary == null ? 0 : summary.failedCount();
        return new RouletteRunSummaryResult(
                run.id(),
                run.ingestionKey(),
                run.userId(),
                run.donorDisplayName(),
                run.donationAmount(),
                Math.toIntExact(roundCount),
                processingStatus(roundCount, appliedCount, failedCount),
                run.createdAt()
        );
    }

    private String processingStatus(long roundCount, long appliedCount, long failedCount) {
        if (roundCount > 0 && appliedCount == roundCount) {
            return "APPLIED";
        }
        if (roundCount > 0 && failedCount == roundCount) {
            return "FAILED";
        }
        if (appliedCount > 0 || failedCount > 0) {
            return "PARTIALLY_APPLIED";
        }
        return "CONFIRMED";
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

    private void requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
    }
}
