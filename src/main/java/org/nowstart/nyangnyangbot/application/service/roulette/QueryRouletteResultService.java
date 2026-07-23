package org.nowstart.nyangnyangbot.application.service.roulette;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RunResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RunRoundSummaryResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueryRouletteResultService implements QueryRouletteResultUseCase {

    private final RoulettePort roulettePort;

    @Override
    public Page<RouletteRunSummaryResult> getRecentRuns(Pageable pageable) {
        Page<RunResult> runs = roulettePort.findRecentRuns(pageable);
        Map<Long, RunRoundSummaryResult> summaries = roulettePort.summarizeRuns(
                        runs.stream().map(RunResult::id).toList()
                ).stream()
                .collect(Collectors.toMap(RunRoundSummaryResult::runId, Function.identity()));
        return runs.map(run -> runSummaryResult(run, summaries.get(run.id())));
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

}
