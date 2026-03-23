package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private FavoriteHistoryRepository favoriteHistoryRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    private List<FavoriteEntity> favoriteEntities;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);

        favoriteEntities = Arrays.asList(
                favoriteEntity("user1", "테스트유저1", 100),
                favoriteEntity("user2", "테스트유저2", 50),
                favoriteEntity("user3", "유저3", 30)
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

    private FavoriteEntity favoriteEntity(String userId, String nickName, int score) {
        FavoriteEntity favoriteEntity = FavoriteEntity.builder()
                .userId(userId)
                .nickName(nickName)
                .build();
        if (ReflectionUtils.findField(FavoriteEntity.class, "totalFavorite") != null) {
            ReflectionTestUtils.setField(favoriteEntity, "totalFavorite", score);
            return favoriteEntity;
        }
        ReflectionTestUtils.setField(favoriteEntity, "favorite", score);
        return favoriteEntity;
    }
}
