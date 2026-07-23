package org.nowstart.nyangnyangbot.application.service.roulette;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;
import org.nowstart.nyangnyangbot.application.service.reward.RewardService;
import org.nowstart.nyangnyangbot.application.service.reward.RewardService.RouletteRewardCommand;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;

class RouletteRoundApplyServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-23T00:00:00Z");

    @Test
    void locksConfirmedRoundGrantsRewardThenMarksApplied() {
        RoulettePort roulettePort = Mockito.mock(RoulettePort.class);
        RewardService rewardService = Mockito.mock(RewardService.class);
        RouletteRoundApplyService service = service(roulettePort, rewardService);
        given(roulettePort.findRoundByIdForUpdate(10L)).willReturn(Optional.of(round(false)));

        service.applyRound(10L);

        then(rewardService).should().grantRoulette(Mockito.any(RouletteRewardCommand.class));
        then(roulettePort).should().markRoundApplied(10L, NOW);
    }

    @Test
    void losingRoundDoesNotCreateRewardGrant() {
        RoulettePort roulettePort = Mockito.mock(RoulettePort.class);
        RewardService rewardService = Mockito.mock(RewardService.class);
        RouletteRoundApplyService service = service(roulettePort, rewardService);
        given(roulettePort.findRoundByIdForUpdate(10L)).willReturn(Optional.of(round(true)));

        service.applyRound(10L);

        then(rewardService).shouldHaveNoInteractions();
        then(roulettePort).should().markRoundApplied(10L, NOW);
    }

    private RouletteRoundApplyService service(RoulettePort roulettePort, RewardService rewardService) {
        return new RouletteRoundApplyService(roulettePort, rewardService) {
            @Override
            Instant now() {
                return NOW;
            }
        };
    }

    private RoundResult round(boolean losing) {
        return new RoundResult(
                10L,
                7L,
                1L,
                "event-1",
                "user-1",
                "시청자",
                losing ? 2L : 1L,
                1,
                losing ? "꽝" : "포인트",
                losing,
                losing ? RewardType.CUSTOM : RewardType.POINT,
                losing ? ConversionMode.NONE : ConversionMode.AUTO,
                losing ? null : 100L,
                RouletteRoundStatus.CONFIRMED,
                null,
                42,
                NOW,
                NOW
        );
    }
}
