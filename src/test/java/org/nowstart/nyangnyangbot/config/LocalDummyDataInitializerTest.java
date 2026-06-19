package org.nowstart.nyangnyangbot.config;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.out.persistence.authorization.repository.AuthorizationRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository.FavoriteRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteTable;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteItemRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteTableRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.repository.UpboTemplateRepository;
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
    private WeeklyChatRankRepository weeklyChatRankRepository;

    @Mock
    private RouletteTableRepository rouletteTableRepository;

    @Mock
    private RouletteItemRepository rouletteItemRepository;

    @Mock
    private UpboTemplateRepository upboTemplateRepository;

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
        BDDMockito.then(authorizationRepository).should().save(any());
        BDDMockito.then(favoriteHistoryRepository).should().saveAll(any());
        BDDMockito.then(weeklyChatRankRepository).should().saveAll(any());
        BDDMockito.then(rouletteItemRepository).should().saveAll(any());
        BDDMockito.then(upboTemplateRepository).should().saveAll(any());
    }

    @Test
    void run_ShouldSkipSeedSectionsWhenDataAlreadyExists() {
        // 준비
        LocalDummyDataInitializer initializer = initializer();
        List<FavoriteAccount> favorites = List.of(favorite("local-channel", "로컬 관리자", 9999));
        given(favoriteRepository.existsById("local-channel")).willReturn(true);
        given(favoriteRepository.existsById("user-001")).willReturn(true);
        given(favoriteRepository.existsById("user-002")).willReturn(true);
        given(favoriteRepository.existsById("user-003")).willReturn(true);
        given(favoriteRepository.existsById("user-004")).willReturn(true);
        given(favoriteRepository.existsById("user-005")).willReturn(true);
        given(favoriteRepository.existsById("user-006")).willReturn(true);
        given(favoriteRepository.existsById("user-007")).willReturn(true);
        given(favoriteRepository.findAllById(anyIterable())).willReturn(favorites);
        given(authorizationRepository.existsById("local-channel")).willReturn(true);
        given(favoriteHistoryRepository.count()).willReturn(1L);
        given(weeklyChatRankRepository.count()).willReturn(1L);
        given(rouletteTableRepository.count()).willReturn(1L);
        given(upboTemplateRepository.count()).willReturn(1L);

        // 실행
        initializer.run(null);

        // 검증
        BDDMockito.then(authorizationRepository).should(never()).save(any());
        BDDMockito.then(favoriteHistoryRepository).should(never()).saveAll(any());
        BDDMockito.then(weeklyChatRankRepository).should(never()).saveAll(any());
        BDDMockito.then(rouletteItemRepository).shouldHaveNoInteractions();
        BDDMockito.then(upboTemplateRepository).should(never()).saveAll(any());
        then(favorites).hasSize(1);
    }

    private LocalDummyDataInitializer initializer() {
        return new LocalDummyDataInitializer(
                authorizationRepository,
                favoriteRepository,
                favoriteHistoryRepository,
                weeklyChatRankRepository,
                rouletteTableRepository,
                rouletteItemRepository,
                upboTemplateRepository
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
