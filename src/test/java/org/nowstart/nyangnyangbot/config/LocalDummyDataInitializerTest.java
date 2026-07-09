package org.nowstart.nyangnyangbot.config;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.out.persistence.authorization.repository.AuthorizationRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository.FavoriteAdjustmentRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository.FavoriteRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteTable;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteEventRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteItemRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRoundResultRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteTableRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.repository.UpboTemplateRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.repository.UserUpboRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.repository.WeeklyChatRankRepository;

@ExtendWith(MockitoExtension.class)
class LocalDummyDataInitializerTest {

    @Mock
    private AuthorizationRepository authorizationRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private FavoriteHistoryRepository favoriteHistoryRepository;

    @Mock
    private FavoriteAdjustmentRepository favoriteAdjustmentRepository;

    @Mock
    private WeeklyChatRankRepository weeklyChatRankRepository;

    @Mock
    private RouletteTableRepository rouletteTableRepository;

    @Mock
    private RouletteItemRepository rouletteItemRepository;

    @Mock
    private RouletteEventRepository rouletteEventRepository;

    @Mock
    private RouletteRoundResultRepository rouletteRoundResultRepository;

    @Mock
    private UpboTemplateRepository upboTemplateRepository;

    @Mock
    private UserUpboRepository userUpboRepository;

    @Test
    void run_ShouldSeedAllLocalDummyDataWhenRepositoriesAreEmpty() {
        // 준비
        LocalDummyDataInitializer initializer = initializer();
        List<FavoriteAccount> favorites = List.of(
                favorite("local-channel", "로컬 관리자", 9999),
                favorite("user-001", "치즈냥", 8720)
        );
        RouletteTable table = RouletteTable.builder()
                .id(1L)
                .title("로컬 테스트 룰렛")
                .command("!룰렛")
                .pricePerRound(1_000L)
                .active(true)
                .version(1)
                .highRoundThreshold(50)
                .build();
        given(favoriteRepository.findAllById(anyIterable())).willReturn(favorites);
        given(rouletteTableRepository.save(any(RouletteTable.class))).willReturn(table);

        // 실행
        initializer.run(null);

        // 검증
        BDDMockito.then(authorizationRepository).should(times(3)).save(any());
        BDDMockito.then(favoriteHistoryRepository).should().saveAll(any());
        BDDMockito.then(weeklyChatRankRepository).should().saveAll(any());
        BDDMockito.then(rouletteItemRepository).should().saveAll(any());
        BDDMockito.then(rouletteEventRepository).should(times(3)).save(any());
        BDDMockito.then(rouletteRoundResultRepository).should(times(3)).saveAll(any());
        BDDMockito.then(upboTemplateRepository).should().saveAll(any());
        BDDMockito.then(userUpboRepository).should().saveAll(any());
        BDDMockito.then(favoriteAdjustmentRepository).should().saveAll(any());
    }

    @Test
    void run_ShouldSkipSeedSectionsWhenDataAlreadyExists() {
        // 준비
        LocalDummyDataInitializer initializer = initializer();
        List<FavoriteAccount> favorites = List.of(favorite("local-channel", "로컬 관리자", 9999));
        given(favoriteRepository.existsById(anyString())).willReturn(true);
        given(favoriteRepository.findAllById(anyIterable())).willReturn(favorites);
        given(authorizationRepository.existsById(anyString())).willReturn(true);
        given(favoriteHistoryRepository.count()).willReturn(1L);
        given(weeklyChatRankRepository.count()).willReturn(1L);
        given(rouletteTableRepository.count()).willReturn(1L);
        given(upboTemplateRepository.count()).willReturn(1L);
        given(userUpboRepository.count()).willReturn(1L);
        given(favoriteAdjustmentRepository.count()).willReturn(1L);

        // 실행
        initializer.run(null);

        // 검증
        BDDMockito.then(authorizationRepository).should(never()).save(any());
        BDDMockito.then(favoriteHistoryRepository).should(never()).saveAll(any());
        BDDMockito.then(weeklyChatRankRepository).should(never()).saveAll(any());
        BDDMockito.then(rouletteItemRepository).shouldHaveNoInteractions();
        BDDMockito.then(rouletteEventRepository).shouldHaveNoInteractions();
        BDDMockito.then(rouletteRoundResultRepository).shouldHaveNoInteractions();
        BDDMockito.then(upboTemplateRepository).should(never()).saveAll(any());
        BDDMockito.then(userUpboRepository).should(never()).saveAll(any());
        BDDMockito.then(favoriteAdjustmentRepository).should(never()).saveAll(any());
        then(favorites).hasSize(1);
    }

    private LocalDummyDataInitializer initializer() {
        return new LocalDummyDataInitializer(
                authorizationRepository,
                favoriteRepository,
                favoriteHistoryRepository,
                favoriteAdjustmentRepository,
                weeklyChatRankRepository,
                rouletteTableRepository,
                rouletteItemRepository,
                rouletteEventRepository,
                rouletteRoundResultRepository,
                upboTemplateRepository,
                userUpboRepository
        );
    }

    private FavoriteAccount favorite(String userId, String nickName, Integer favorite) {
        return FavoriteAccount.builder()
                .userId(userId)
                .nickName(nickName)
                .favorite(favorite)
                .build();
    }
}
