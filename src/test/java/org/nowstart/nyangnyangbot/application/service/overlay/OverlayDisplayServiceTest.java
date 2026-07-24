package org.nowstart.nyangnyangbot.application.service.overlay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayDisplayPort;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayDisplayPort.DisplayJobResult;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayDisplayPort.DisplayRoundResult;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;

class OverlayDisplayServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-23T00:00:00Z");

    @Test
    void claimReturnsClaimTokenThatCompletionMustEcho() {
        OverlayTokenService tokenService = Mockito.mock(OverlayTokenService.class);
        OverlayDisplayPort port = Mockito.mock(OverlayDisplayPort.class);
        OverlayDisplayService service = service(tokenService, port);
        given(tokenService.validateToken("secret")).willReturn(true);
        given(port.claimNext(any(), any(), any())).willAnswer(invocation -> Optional.of(job(
                invocation.getArgument(1)
        )));

        var claimed = service.claimNextJob("Bearer secret").orElseThrow();
        service.markDisplayed(claimed.displayJobId(), claimed.claimToken(), "Bearer secret");

        then(port).should().markExpiredMissed(NOW);
        then(port).should().markDisplayed(1L, "claim-1", NOW);
        assertThat(claimed.claimToken()).isEqualTo("claim-1");
    }

    @Test
    void enqueueUsesStableRunIdempotencyKey() {
        OverlayTokenService tokenService = Mockito.mock(OverlayTokenService.class);
        OverlayDisplayPort port = Mockito.mock(OverlayDisplayPort.class);
        OverlayDisplayService service = service(tokenService, port);

        service.enqueueRouletteRun(9L);

        then(port).should().enqueue(9L, "roulette-run:9", NOW.plusSeconds(120), NOW);
    }

    @Test
    void replayCreatesAReplayJobWithoutReturningItsDisplayPayload() {
        OverlayTokenService tokenService = Mockito.mock(OverlayTokenService.class);
        OverlayDisplayPort port = Mockito.mock(OverlayDisplayPort.class);
        OverlayDisplayService service = service(tokenService, port);
        given(port.replay(Mockito.eq(9L), Mockito.anyString(), Mockito.eq(NOW.plusSeconds(120)), Mockito.eq(NOW)))
                .willReturn(2L);

        service.replayRouletteRun(9L);

        then(port).should().replay(
                9L,
                "roulette-run:9:replay:claim-1",
                NOW.plusSeconds(120),
                NOW
        );
    }

    private OverlayDisplayService service(OverlayTokenService tokenService, OverlayDisplayPort port) {
        return new OverlayDisplayService(tokenService, port) {
            @Override
            Instant now() {
                return NOW;
            }

            @Override
            String newClaimToken() {
                return "claim-1";
            }
        };
    }

    private DisplayJobResult job(String claimToken) {
        return new DisplayJobResult(
                1L,
                "후원자",
                claimToken,
                1,
                List.of(new DisplayRoundResult(
                        2L,
                        1,
                        "포인트",
                        false,
                        RewardType.POINT,
                        ConversionMode.AUTO,
                        100L,
                        RouletteRoundStatus.APPLIED,
                        null
                ))
        );
    }
}
