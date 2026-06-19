package org.nowstart.nyangnyangbot.application.service.roulette;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import org.mockito.BDDMockito;
import static org.mockito.Mockito.never;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.FavoriteLedgerResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.UserResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.CreateUserUpboCommand;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort;
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
        // 준비
        RouletteRoundApplyService service = createService();
        RoundResult round = favoriteRound(false);
        given(roulettePort.findRoundById(30L)).willReturn(Optional.of(round));
        given(adjustFavoriteUseCase.adjust(any(AdjustFavoriteCommand.class)))
                .willReturn(new FavoriteLedgerResult("user-1", 20, 10, 30, "룰렛 결과", false, 99L));
        given(upboPort.createUserUpbo(any(CreateUserUpboCommand.class)))
                .willReturn(userUpbo(40L, 99L));

        // 실행
        service.applyRound(30L);

        // 검증
        BDDMockito.then(roulettePort).should().markRoundApplied(30L, 99L, 40L);
        BDDMockito.then(adjustFavoriteUseCase).should().adjust(argThat(command ->
                "user-1".equals(command.userId())
                        && command.delta() == 10
                        && command.sourceType() == FavoriteSourceType.UPBO_ROULETTE
                        && "roulette-round:30".equals(command.idempotencyKey())
        ));
    }

    @Test
    void applyRound_ShouldMarkLosingRoundAppliedWithoutLedgerOrUpbo() {
        // 준비
        RouletteRoundApplyService service = createService();
        RoundResult round = favoriteRound(true);
        given(roulettePort.findRoundById(30L)).willReturn(Optional.of(round));

        // 실행
        service.applyRound(30L);

        // 검증
        BDDMockito.then(roulettePort).should().markRoundApplied(30L, null, null);
        BDDMockito.then(adjustFavoriteUseCase).should(never()).adjust(any());
        BDDMockito.then(upboPort).should(never()).createUserUpbo(any());
    }

    @Test
    void applyRound_ShouldIgnoreAlreadyAppliedRound() {
        // 준비
        RouletteRoundApplyService service = createService();
        RoundResult round = round(false, RewardType.FAVORITE, ConversionMode.AUTO, 10, RouletteRoundStatus.APPLIED);
        given(roulettePort.findRoundById(30L)).willReturn(Optional.of(round));

        // 실행
        service.applyRound(30L);

        // 검증
        BDDMockito.then(adjustFavoriteUseCase).shouldHaveNoInteractions();
        BDDMockito.then(upboPort).shouldHaveNoInteractions();
        BDDMockito.then(roulettePort).should(never()).markRoundApplied(any(), any(), any());
    }

    @Test
    void applyRound_ShouldRejectMissingRound() {
        // 준비
        RouletteRoundApplyService service = createService();
        given(roulettePort.findRoundById(404L)).willReturn(Optional.empty());

        // 실행 및 검증
        thenThrownBy(() -> service.applyRound(404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("roulette round not found");
    }

    @Test
    void applyRound_ShouldCreateOwnedUpboWithoutLedgerForManualConversion() {
        // 준비
        RouletteRoundApplyService service = createService();
        RoundResult round = round(false, RewardType.MISSION, ConversionMode.MANUAL, null, RouletteRoundStatus.CONFIRMED);
        given(roulettePort.findRoundById(30L)).willReturn(Optional.of(round));
        given(upboPort.createUserUpbo(any(CreateUserUpboCommand.class)))
                .willReturn(userUpbo(41L, null, UpboStatus.OWNED));

        // 실행
        service.applyRound(30L);

        // 검증
        BDDMockito.then(adjustFavoriteUseCase).should(never()).adjust(any());
        BDDMockito.then(upboPort).should().createUserUpbo(argThat(command ->
                command.status() == UpboStatus.OWNED
                        && command.ledgerId() == null
                        && command.exchangeFavoriteValue() == null
        ));
        BDDMockito.then(roulettePort).should().markRoundApplied(30L, null, 41L);
    }

    @Test
    void applyRound_ShouldPropagateExceptionWhenApplyingRewardThrows() {
        // 준비
        RouletteRoundApplyService service = createService();
        RoundResult round = favoriteRound(false);
        given(roulettePort.findRoundById(30L)).willReturn(Optional.of(round));
        given(adjustFavoriteUseCase.adjust(any(AdjustFavoriteCommand.class)))
                .willThrow(new IllegalStateException("잔액 반영 실패"));

        // 실행 및 검증
        thenThrownBy(() -> service.applyRound(30L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("잔액 반영 실패");
        BDDMockito.then(roulettePort).should(never()).markRoundFailed(any(), any());
    }

    private RouletteRoundApplyService createService() {
        return new RouletteRoundApplyService(
                roulettePort,
                upboPort,
                adjustFavoriteUseCase
        );
    }

    private RoundResult favoriteRound(boolean losingItem) {
        return round(
                losingItem,
                losingItem ? RewardType.CUSTOM : RewardType.FAVORITE,
                losingItem ? ConversionMode.NONE : ConversionMode.AUTO,
                losingItem ? null : 10,
                RouletteRoundStatus.CONFIRMED
        );
    }

    private RoundResult round(
            boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            RouletteRoundStatus status
    ) {
        return new RoundResult(
                30L,
                20L,
                "donation-1",
                "user-1",
                "치즈냥",
                1,
                losingItem ? "꽝" : "호감도 +10",
                10_000,
                losingItem,
                rewardType,
                conversionMode,
                exchangeFavoriteValue,
                status,
                null,
                null,
                null,
                1
        );
    }

    private UserResult userUpbo(Long id, Long ledgerId) {
        return userUpbo(id, ledgerId, UpboStatus.CONVERTED);
    }

    private UserResult userUpbo(Long id, Long ledgerId, UpboStatus status) {
        return new UserResult(
                id,
                "user-1",
                null,
                "치즈냥",
                "호감도 +10",
                status,
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
