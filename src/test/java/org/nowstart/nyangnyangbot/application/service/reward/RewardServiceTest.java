package org.nowstart.nyangnyangbot.application.service.reward;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase.AdjustPointCommand;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase.PointLedgerResult;
import org.nowstart.nyangnyangbot.application.port.in.point.GrantPointUseCase;
import org.nowstart.nyangnyangbot.application.port.out.reward.RewardPort;
import org.nowstart.nyangnyangbot.application.port.out.reward.RewardPort.CreateRewardCommand;
import org.nowstart.nyangnyangbot.application.port.out.reward.RewardPort.RewardRecord;
import org.nowstart.nyangnyangbot.application.service.reward.RewardService.RouletteRewardCommand;
import org.nowstart.nyangnyangbot.domain.point.PointSourceType;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardGrantStatus;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

class RewardServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-23T00:00:00Z");

    @Test
    void rouletteAutoRewardUsesSameStableIdempotencyForPointAndGrant() {
        RewardPort rewardPort = Mockito.mock(RewardPort.class);
        GrantPointUseCase pointUseCase = Mockito.mock(GrantPointUseCase.class);
        RewardService service = service(rewardPort, pointUseCase);
        given(rewardPort.findByRouletteRoundId(10L)).willReturn(Optional.empty());
        given(pointUseCase.grant(Mockito.any())).willReturn(new PointLedgerResult(
                "user-1", 0, 100, 100, "룰렛 결과", false, 20L
        ));
        given(rewardPort.createGrant(Mockito.any())).willReturn(reward(30L, 10L, 20L));

        RewardRecord result = service.grantRoulette(new RouletteRewardCommand(
                10L,
                "user-1",
                "시청자",
                "event-1",
                "포인트",
                RewardType.POINT,
                ConversionMode.AUTO,
                100L,
                "룰렛 결과",
                "roundNo=1"
        ));

        ArgumentCaptor<AdjustPointCommand> point = ArgumentCaptor.forClass(AdjustPointCommand.class);
        then(pointUseCase).should().grant(point.capture());
        ArgumentCaptor<CreateRewardCommand> grant = ArgumentCaptor.forClass(CreateRewardCommand.class);
        then(rewardPort).should().createGrant(grant.capture());
        assertThat(point.getValue().sourceType()).isEqualTo(PointSourceType.REWARD_ROULETTE);
        assertThat(point.getValue().idempotencyKey()).isEqualTo("roulette-round:10");
        assertThat(grant.getValue().idempotencyKey()).isEqualTo("roulette-round:10");
        assertThat(result.id()).isEqualTo(30L);
    }

    @Test
    void getUserRewards_PropagatesBoundToPersistenceQuery() {
        RewardPort rewardPort = Mockito.mock(RewardPort.class);
        GrantPointUseCase pointUseCase = Mockito.mock(GrantPointUseCase.class);
        RewardService service = service(rewardPort, pointUseCase);
        given(rewardPort.findByUserId("user-1", 20)).willReturn(List.of(reward(30L, 10L, 20L)));

        var result = service.getUserRewards("user-1", null, 20);

        assertThat(result).hasSize(1);
        then(rewardPort).should().findByUserId("user-1", 20);
    }

    @Test
    void getUserRewards_RejectsUnboundedLimit() {
        RewardPort rewardPort = Mockito.mock(RewardPort.class);
        RewardService service = service(rewardPort, Mockito.mock(GrantPointUseCase.class));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getUserRewards("user-1", null, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("reward query limit must be between 1 and 100");

        then(rewardPort).shouldHaveNoInteractions();
    }

    private RewardService service(RewardPort rewardPort, GrantPointUseCase pointUseCase) {
        return new RewardService(rewardPort, pointUseCase) {
            @Override
            Instant now() {
                return NOW;
            }
        };
    }

    private RewardRecord reward(Long id, Long roundId, Long ledgerId) {
        return new RewardRecord(
                id,
                "user-1",
                roundId,
                ledgerId,
                "포인트",
                RewardType.POINT,
                ConversionMode.AUTO,
                100L,
                RewardGrantStatus.CONVERTED,
                "룰렛 결과",
                "roundNo=1",
                null,
                "roulette-round:10",
                NOW,
                NOW
        );
    }
}
