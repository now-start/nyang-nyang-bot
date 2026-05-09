package org.nowstart.nyangnyangbot.service;

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
import org.nowstart.nyangnyangbot.data.entity.RouletteEventEntity;
import org.nowstart.nyangnyangbot.data.entity.RouletteRoundResultEntity;
import org.nowstart.nyangnyangbot.data.entity.UserUpboEntity;
import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.RewardType;
import org.nowstart.nyangnyangbot.data.type.RouletteRoundStatus;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.repository.RouletteRoundResultRepository;
import org.nowstart.nyangnyangbot.repository.UserUpboRepository;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RouletteRoundApplyServiceTest {

    @Mock
    private RouletteRoundResultRepository rouletteRoundResultRepository;

    @Mock
    private UserUpboRepository userUpboRepository;

    @Mock
    private AdjustFavoriteUseCase adjustFavoriteUseCase;

    @Test
    void applyRound_ShouldConvertAutoFavoriteRewardThroughLedger() {
        RouletteRoundApplyService service = createService();
        RouletteRoundResultEntity round = favoriteRound(false);
        given(rouletteRoundResultRepository.findById(30L)).willReturn(Optional.of(round));
        given(adjustFavoriteUseCase.adjust(any(AdjustFavoriteCommand.class)))
                .willReturn(new FavoriteLedgerResult("user-1", 20, 10, 30, "룰렛 결과", false, 99L));
        given(userUpboRepository.save(any(UserUpboEntity.class))).willAnswer(invocation -> {
            UserUpboEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 40L);
            return entity;
        });

        service.applyRound(30L);

        assertThat(round.getStatus()).isEqualTo(RouletteRoundStatus.APPLIED);
        assertThat(round.getLedgerId()).isEqualTo(99L);
        assertThat(round.getUserUpboId()).isEqualTo(40L);
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
        RouletteRoundResultEntity round = favoriteRound(true);
        given(rouletteRoundResultRepository.findById(30L)).willReturn(Optional.of(round));

        service.applyRound(30L);

        assertThat(round.getStatus()).isEqualTo(RouletteRoundStatus.APPLIED);
        assertThat(round.getLedgerId()).isNull();
        assertThat(round.getUserUpboId()).isNull();
        then(adjustFavoriteUseCase).should(never()).adjust(any());
        then(userUpboRepository).should(never()).save(any());
    }

    private RouletteRoundApplyService createService() {
        return new RouletteRoundApplyService(
                rouletteRoundResultRepository,
                userUpboRepository,
                adjustFavoriteUseCase
        );
    }

    private RouletteRoundResultEntity favoriteRound(boolean losingItem) {
        RouletteEventEntity event = RouletteEventEntity.builder()
                .id(20L)
                .donationEventId("donation-1")
                .userId("user-1")
                .nickNameSnapshot("치즈냥")
                .build();
        RouletteRoundResultEntity round = RouletteRoundResultEntity.builder()
                .id(30L)
                .rouletteEvent(event)
                .roundNo(1)
                .itemLabel(losingItem ? "꽝" : "호감도 +10")
                .losingItem(losingItem)
                .rewardType(losingItem ? RewardType.CUSTOM : RewardType.FAVORITE)
                .conversionMode(losingItem ? ConversionMode.NONE : ConversionMode.AUTO)
                .exchangeFavoriteValue(losingItem ? null : 10)
                .status(RouletteRoundStatus.CONFIRMED)
                .build();
        return round;
    }
}
