package org.nowstart.nyangnyangbot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.favorite.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.favorite.AdjustFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.favorite.FavoriteLedgerResult;
import org.nowstart.nyangnyangbot.domain.model.RouletteRound;
import org.nowstart.nyangnyangbot.domain.model.UserUpbo;
import org.nowstart.nyangnyangbot.application.gateway.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.gateway.out.upbo.CreateUserUpboCommand;
import org.nowstart.nyangnyangbot.application.gateway.out.upbo.UpboPort;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;

@ExtendWith(MockitoExtension.class)
class RouletteRoundApplyServiceTest {

    @Mock
    private RoulettePort roulettePort;

    @Mock
    private UpboPort upboPort;

    @Mock
    private AdjustFavoriteUseCase adjustFavoriteUseCase;

    @Test
    void applyRound_ShouldConvertAutoFavoriteRewardThroughLedger() {
        RouletteRoundApplyService service = createService();
        RouletteRound round = favoriteRound(false);
        given(roulettePort.findRoundById(30L)).willReturn(Optional.of(round));
        given(adjustFavoriteUseCase.adjust(any(AdjustFavoriteCommand.class)))
                .willReturn(new FavoriteLedgerResult("user-1", 20, 10, 30, "룰렛 결과", false, 99L));
        given(upboPort.createUserUpbo(any(CreateUserUpboCommand.class)))
                .willReturn(userUpbo(40L, 99L));

        service.applyRound(30L);

        then(roulettePort).should().markRoundApplied(30L, 99L, 40L);
        then(adjustFavoriteUseCase).should().adjust(argThat(command ->
                "user-1".equals(command.userId())
                        && command.delta() == 10
                        && command.sourceType() == FavoriteSourceType.UPBO_ROULETTE
                        && "roulette-round:30".equals(command.idempotencyKey())
        ));
    }

    @Test
    void applyRound_ShouldMarkLosingRoundAppliedWithoutLedgerOrUpbo() {
        RouletteRoundApplyService service = createService();
        RouletteRound round = favoriteRound(true);
        given(roulettePort.findRoundById(30L)).willReturn(Optional.of(round));

        service.applyRound(30L);

        then(roulettePort).should().markRoundApplied(30L, null, null);
        then(adjustFavoriteUseCase).should(never()).adjust(any());
        then(upboPort).should(never()).createUserUpbo(any());
    }

    private RouletteRoundApplyService createService() {
        return new RouletteRoundApplyService(
                roulettePort,
                upboPort,
                adjustFavoriteUseCase
        );
    }

    private RouletteRound favoriteRound(boolean losingItem) {
        return new RouletteRound(
                30L,
                20L,
                "donation-1",
                "user-1",
                "치즈냥",
                1,
                losingItem ? "꽝" : "호감도 +10",
                10_000,
                losingItem,
                losingItem ? RewardType.CUSTOM : RewardType.FAVORITE,
                losingItem ? ConversionMode.NONE : ConversionMode.AUTO,
                losingItem ? null : 10,
                RouletteRoundStatus.CONFIRMED,
                null,
                null,
                null,
                1
        );
    }

    private UserUpbo userUpbo(Long id, Long ledgerId) {
        return new UserUpbo(
                id,
                "user-1",
                null,
                "치즈냥",
                "호감도 +10",
                UpboStatus.CONVERTED,
                10,
                RewardType.FAVORITE,
                ConversionMode.AUTO,
                FavoriteSourceType.UPBO_ROULETTE,
                ledgerId,
                "룰렛 결과: 호감도 +10",
                "관리자 확인",
                "SYSTEM",
                null
        );
    }
}
