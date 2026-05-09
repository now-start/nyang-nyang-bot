package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.dto.favorite.FavoriteMeDto;
import org.nowstart.nyangnyangbot.data.entity.AuthorizationEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;
import org.nowstart.nyangnyangbot.repository.AuthorizationRepository;
import org.nowstart.nyangnyangbot.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private FavoriteHistoryRepository favoriteHistoryRepository;

    @Mock
    private AuthorizationRepository authorizationRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    private List<FavoriteEntity> favoriteEntities;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);

        favoriteEntities = Arrays.asList(
                FavoriteEntity.builder()
                        .userId("user1")
                        .nickName("테스트유저1")
                        .favorite(100)
                        .build(),
                FavoriteEntity.builder()
                        .userId("user2")
                        .nickName("테스트유저2")
                        .favorite(50)
                        .build(),
                FavoriteEntity.builder()
                        .userId("user3")
                        .nickName("유저3")
                        .favorite(30)
                        .build()
        );
    }

    @Test
    void getList_ShouldReturnAllFavorites() {
        // given
        Page<FavoriteEntity> expectedPage = new PageImpl<>(favoriteEntities, pageable, favoriteEntities.size());
        given(favoriteRepository.findAll(pageable)).willReturn(expectedPage);

        // when
        Page<FavoriteEntity> result = favoriteService.getList(pageable);

        // then
        then(result).isNotNull();
        then(result.getContent()).hasSize(3);
        then(result.getTotalElements()).isEqualTo(3);
        BDDMockito.then(favoriteRepository).should().findAll(pageable);
    }

    @Test
    void getList_ShouldReturnEmptyPage_WhenNoFavorites() {
        // given
        Page<FavoriteEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteRepository.findAll(pageable)).willReturn(emptyPage);

        // when
        Page<FavoriteEntity> result = favoriteService.getList(pageable);

        // then
        then(result).isNotNull();
        then(result.getContent()).isEmpty();
        then(result.getTotalElements()).isZero();
        BDDMockito.then(favoriteRepository).should().findAll(pageable);
    }

    @Test
    void getByNickName_ShouldReturnFilteredFavorites() {
        // given
        String nickName = "테스트";
        List<FavoriteEntity> filteredList = Arrays.asList(
                favoriteEntities.get(0),
                favoriteEntities.get(1)
        );
        Page<FavoriteEntity> expectedPage = new PageImpl<>(filteredList, pageable, filteredList.size());
        given(favoriteRepository.findByNickNameContains(pageable, nickName)).willReturn(expectedPage);

        // when
        Page<FavoriteEntity> result = favoriteService.getByNickName(pageable, nickName);

        // then
        then(result).isNotNull();
        then(result.getContent()).hasSize(2);
        then(result.getContent()).allMatch(entity -> entity.getNickName().contains(nickName));
        BDDMockito.then(favoriteRepository).should().findByNickNameContains(pageable, nickName);
    }

    @Test
    void getByNickName_ShouldReturnEmptyPage_WhenNoMatch() {
        // given
        String nickName = "존재하지않는유저";
        Page<FavoriteEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteRepository.findByNickNameContains(pageable, nickName)).willReturn(emptyPage);

        // when
        Page<FavoriteEntity> result = favoriteService.getByNickName(pageable, nickName);

        // then
        then(result).isNotNull();
        then(result.getContent()).isEmpty();
        BDDMockito.then(favoriteRepository).should().findByNickNameContains(pageable, nickName);
    }

    @Test
    void getByNickName_ShouldHandleSpecialCharacters() {
        // given
        String nickName = "유저@#$";
        Page<FavoriteEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteRepository.findByNickNameContains(pageable, nickName)).willReturn(emptyPage);

        // when
        Page<FavoriteEntity> result = favoriteService.getByNickName(pageable, nickName);

        // then
        then(result).isNotNull();
        BDDMockito.then(favoriteRepository).should().findByNickNameContains(pageable, nickName);
    }

    @Test
    void getList_ShouldHandleDifferentPageSizes() {
        // given
        Pageable largePageable = PageRequest.of(0, 100);
        Page<FavoriteEntity> expectedPage = new PageImpl<>(favoriteEntities, largePageable, favoriteEntities.size());
        given(favoriteRepository.findAll(largePageable)).willReturn(expectedPage);

        // when
        Page<FavoriteEntity> result = favoriteService.getList(largePageable);

        // then
        then(result).isNotNull();
        then(result.getContent()).hasSize(3);
        BDDMockito.then(favoriteRepository).should().findAll(largePageable);
    }

    @Test
    void getMyFavorite_ShouldReturnSummaryAndMarkSeen() {
        AuthorizationEntity authorization = AuthorizationEntity.builder()
                .channelId("user1")
                .channelName("치즈냥")
                .admin(false)
                .build();
        FavoriteEntity favorite = FavoriteEntity.builder()
                .userId("user1")
                .nickName("치즈냥")
                .favorite(42)
                .build();
        FavoriteHistoryEntity history = FavoriteHistoryEntity.builder()
                .favorite(42)
                .history("출석체크(+1)")
                .build();
        Page<FavoriteHistoryEntity> historyPage = new PageImpl<>(List.of(history));

        given(authorizationRepository.findById("user1")).willReturn(Optional.of(authorization));
        given(favoriteRepository.findById("user1")).willReturn(Optional.of(favorite));
        given(favoriteHistoryRepository.countByFavoriteEntityUserId("user1")).willReturn(1L);
        given(favoriteHistoryRepository.findByFavoriteEntityUserId(BDDMockito.eq("user1"), BDDMockito.any(Pageable.class)))
                .willReturn(historyPage);

        FavoriteMeDto result = favoriteService.getMyFavorite("user1");

        then(result.userId()).isEqualTo("user1");
        then(result.nickName()).isEqualTo("치즈냥");
        then(result.favorite()).isEqualTo(42);
        then(result.unseenCount()).isEqualTo(1L);
        then(result.histories()).hasSize(1);
        then(authorization.getFavoriteHistoryLastSeenAt()).isNotNull();
    }
}
