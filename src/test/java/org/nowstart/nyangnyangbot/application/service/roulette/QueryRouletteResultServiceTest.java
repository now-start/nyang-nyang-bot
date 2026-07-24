package org.nowstart.nyangnyangbot.application.service.roulette;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RunResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RunRoundSummaryResult;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class QueryRouletteResultServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-23T00:00:00Z");

    @Test
    void recentRunsUsesOneGroupedRoundSummaryInsteadOfLoadingRoundsPerRun() {
        RoulettePort port = Mockito.mock(RoulettePort.class);
        QueryRouletteResultService service = new QueryRouletteResultService(port);
        PageRequest pageRequest = PageRequest.of(0, 20);
        given(port.findRecentRuns(pageRequest)).willReturn(new PageImpl<>(
                List.of(run(7L), run(8L)), pageRequest, 2
        ));
        given(port.summarizeRuns(List.of(7L, 8L))).willReturn(List.of(
                new RunRoundSummaryResult(7L, 1000, 999, 1),
                new RunRoundSummaryResult(8L, 2, 2, 0)
        ));

        var result = service.getRecentRuns(pageRequest);

        assertThat(result.getContent()).extracting(summary -> summary.status())
                .containsExactly("PARTIALLY_APPLIED", "APPLIED");
        assertThat(result.getContent()).extracting(summary -> summary.roundCount())
                .containsExactly(1000, 2);
        then(port).should().summarizeRuns(List.of(7L, 8L));
        then(port).should(Mockito.never()).findRoundsByRunId(Mockito.anyLong());
    }

    private RunResult run(Long id) {
        return new RunResult(
                id,
                "receipt-" + id,
                "user-1",
                "시청자",
                1_000L,
                NOW
        );
    }
}
