package org.nowstart.nyangnyangbot.application.service;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.model.AuthorizationAccount;
import org.nowstart.nyangnyangbot.application.model.FavoriteHistoryView;
import org.nowstart.nyangnyangbot.application.model.FavoriteSummary;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort;
import org.nowstart.nyangnyangbot.application.dto.favorite.FavoriteMeDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteQueryPort favoriteQueryPort;

    @Mock
    private AuthorizationPort authorizationPort;

    @InjectMocks
    private FavoriteService favoriteService;

    private List<FavoriteSummary> favoriteEntities;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);

        favoriteEntities = List.of(
                new FavoriteSummary("user1", "테스트유저1", 100),
                new FavoriteSummary("user2", "테스트유저2", 50),
                new FavoriteSummary("user3", "유저3", 30)
        );
    }

    @Test
    void getList_ShouldReturnAllFavorites() {
        // given
        Page<FavoriteSummary> expectedPage = new PageImpl<>(favoriteEntities, pageable, favoriteEntities.size());
        given(favoriteQueryPort.findAll(pageable)).willReturn(expectedPage);

        // when
        Page<FavoriteSummary> result = favoriteService.getList(pageable);

        // then
        then(result).isNotNull();
        then(result.getContent()).hasSize(3);
        then(result.getTotalElements()).isEqualTo(3);
        BDDMockito.then(favoriteQueryPort).should().findAll(pageable);
    }

    @Test
    void getList_ShouldReturnEmptyPage_WhenNoFavorites() {
        // given
        Page<FavoriteSummary> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteQueryPort.findAll(pageable)).willReturn(emptyPage);

        // when
        Page<FavoriteSummary> result = favoriteService.getList(pageable);

        // then
        then(result).isNotNull();
        then(result.getContent()).isEmpty();
        then(result.getTotalElements()).isZero();
        BDDMockito.then(favoriteQueryPort).should().findAll(pageable);
    }

    @Test
    void getByNickName_ShouldReturnFilteredFavorites() {
        // given
        String nickName = "테스트";
        List<FavoriteSummary> filteredList = List.of(
                favoriteEntities.get(0),
                favoriteEntities.get(1)
        );
        Page<FavoriteSummary> expectedPage = new PageImpl<>(filteredList, pageable, filteredList.size());
        given(favoriteQueryPort.findByNickNameContains(pageable, nickName)).willReturn(expectedPage);

        // when
        Page<FavoriteSummary> result = favoriteService.getByNickName(pageable, nickName);

        // then
        then(result).isNotNull();
        then(result.getContent()).hasSize(2);
        then(result.getContent()).allMatch(entity -> entity.nickName().contains(nickName));
        BDDMockito.then(favoriteQueryPort).should().findByNickNameContains(pageable, nickName);
    }

    @Test
    void getByNickName_ShouldReturnEmptyPage_WhenNoMatch() {
        // given
        String nickName = "존재하지않는유저";
        Page<FavoriteSummary> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteQueryPort.findByNickNameContains(pageable, nickName)).willReturn(emptyPage);

        // when
        Page<FavoriteSummary> result = favoriteService.getByNickName(pageable, nickName);

        // then
        then(result).isNotNull();
        then(result.getContent()).isEmpty();
        BDDMockito.then(favoriteQueryPort).should().findByNickNameContains(pageable, nickName);
    }

    @Test
    void getByNickName_ShouldHandleSpecialCharacters() {
        // given
        String nickName = "유저@#$";
        Page<FavoriteSummary> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteQueryPort.findByNickNameContains(pageable, nickName)).willReturn(emptyPage);

        // when
        Page<FavoriteSummary> result = favoriteService.getByNickName(pageable, nickName);

        // then
        then(result).isNotNull();
        BDDMockito.then(favoriteQueryPort).should().findByNickNameContains(pageable, nickName);
    }

    @Test
    void getList_ShouldHandleDifferentPageSizes() {
        // given
        Pageable largePageable = PageRequest.of(0, 100);
        Page<FavoriteSummary> expectedPage = new PageImpl<>(favoriteEntities, largePageable, favoriteEntities.size());
        given(favoriteQueryPort.findAll(largePageable)).willReturn(expectedPage);

        // when
        Page<FavoriteSummary> result = favoriteService.getList(largePageable);

        // then
        then(result).isNotNull();
        then(result.getContent()).hasSize(3);
        BDDMockito.then(favoriteQueryPort).should().findAll(largePageable);
    }

    @Test
    void getMyFavorite_ShouldReturnSummaryAndMarkSeen() {
        AuthorizationAccount authorization = new AuthorizationAccount(
                "user1", "치즈냥", null, null, null, null, null, false, null, null
        );
        FavoriteSummary favorite = new FavoriteSummary("user1", "치즈냥", 42);
        FavoriteHistoryView history = new FavoriteHistoryView(
                1L, "user1", "치즈냥", 1, 42, null, null,
                "출석체크(+1)", false, 42, "출석체크(+1)", LocalDateTime.now()
        );

        given(authorizationPort.findById("user1")).willReturn(Optional.of(authorization));
        given(favoriteQueryPort.findById("user1")).willReturn(Optional.of(favorite));
        given(favoriteQueryPort.countHistory("user1")).willReturn(1L);
        given(favoriteQueryPort.findHistory("user1", 50)).willReturn(List.of(history));

        FavoriteMeDto result = favoriteService.getMyFavorite("user1");

        then(result.userId()).isEqualTo("user1");
        then(result.nickName()).isEqualTo("치즈냥");
        then(result.favorite()).isEqualTo(42);
        then(result.unseenCount()).isEqualTo(1L);
        then(result.histories()).hasSize(1);
        BDDMockito.then(authorizationPort).should().markFavoriteHistorySeen(BDDMockito.eq("user1"), BDDMockito.any(LocalDateTime.class));
    }
}
