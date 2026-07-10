package org.nowstart.nyangnyangbot.application.service.favorite;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;

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
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort.AuthorizationAccountResult;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort.HistoryResult;
import org.nowstart.nyangnyangbot.application.port.in.favorite.QueryFavoriteUseCase.FavoriteSummaryResult;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort.SummaryResult;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort;
import org.nowstart.nyangnyangbot.application.port.in.favorite.QueryFavoriteUseCase.FavoriteMeResult;
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

    private List<SummaryResult> favoriteEntities;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);

        favoriteEntities = List.of(
                new SummaryResult("user1", "테스트유저1", 100),
                new SummaryResult("user2", "테스트유저2", 50),
                new SummaryResult("user3", "유저3", 30)
        );
    }

    @Test
    void getList_ShouldReturnAllFavorites() {
        // 준비
        Page<SummaryResult> expectedPage = new PageImpl<>(favoriteEntities, pageable, favoriteEntities.size());
        given(favoriteQueryPort.findAll(pageable)).willReturn(expectedPage);

        // 실행
        Page<FavoriteSummaryResult> result = favoriteService.getList(pageable);

        // 검증
        then(result).isNotNull();
        then(result.getContent()).hasSize(3);
        then(result.getTotalElements()).isEqualTo(3);
        BDDMockito.then(favoriteQueryPort).should().findAll(pageable);
    }

    @Test
    void getList_ShouldReturnEmptyPage_WhenNoFavorites() {
        // 준비
        Page<SummaryResult> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteQueryPort.findAll(pageable)).willReturn(emptyPage);

        // 실행
        Page<FavoriteSummaryResult> result = favoriteService.getList(pageable);

        // 검증
        then(result).isNotNull();
        then(result.getContent()).isEmpty();
        then(result.getTotalElements()).isZero();
        BDDMockito.then(favoriteQueryPort).should().findAll(pageable);
    }

    @Test
    void getByNickName_ShouldReturnFilteredFavorites() {
        // 준비
        String nickName = "테스트";
        List<SummaryResult> filteredList = List.of(
                favoriteEntities.get(0),
                favoriteEntities.get(1)
        );
        Page<SummaryResult> expectedPage = new PageImpl<>(filteredList, pageable, filteredList.size());
        given(favoriteQueryPort.findByNickNameContains(pageable, nickName)).willReturn(expectedPage);

        // 실행
        Page<FavoriteSummaryResult> result = favoriteService.getByNickName(pageable, nickName);

        // 검증
        then(result).isNotNull();
        then(result.getContent()).hasSize(2);
        then(result.getContent()).allMatch(entity -> entity.nickName().contains(nickName));
        BDDMockito.then(favoriteQueryPort).should().findByNickNameContains(pageable, nickName);
    }

    @Test
    void getByNickName_ShouldReturnEmptyPage_WhenNoMatch() {
        // 준비
        String nickName = "존재하지않는유저";
        Page<SummaryResult> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteQueryPort.findByNickNameContains(pageable, nickName)).willReturn(emptyPage);

        // 실행
        Page<FavoriteSummaryResult> result = favoriteService.getByNickName(pageable, nickName);

        // 검증
        then(result).isNotNull();
        then(result.getContent()).isEmpty();
        BDDMockito.then(favoriteQueryPort).should().findByNickNameContains(pageable, nickName);
    }

    @Test
    void getByNickName_ShouldHandleSpecialCharacters() {
        // 준비
        String nickName = "유저@#$";
        Page<SummaryResult> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteQueryPort.findByNickNameContains(pageable, nickName)).willReturn(emptyPage);

        // 실행
        Page<FavoriteSummaryResult> result = favoriteService.getByNickName(pageable, nickName);

        // 검증
        then(result).isNotNull();
        BDDMockito.then(favoriteQueryPort).should().findByNickNameContains(pageable, nickName);
    }

    @Test
    void getList_ShouldHandleDifferentPageSizes() {
        // 준비
        Pageable largePageable = PageRequest.of(0, 100);
        Page<SummaryResult> expectedPage = new PageImpl<>(favoriteEntities, largePageable, favoriteEntities.size());
        given(favoriteQueryPort.findAll(largePageable)).willReturn(expectedPage);

        // 실행
        Page<FavoriteSummaryResult> result = favoriteService.getList(largePageable);

        // 검증
        then(result).isNotNull();
        then(result.getContent()).hasSize(3);
        BDDMockito.then(favoriteQueryPort).should().findAll(largePageable);
    }

    @Test
    void getMyFavorite_ShouldReturnSummaryWithoutMarkingSeen() {
        // 준비
        AuthorizationAccountResult authorization = new AuthorizationAccountResult(
                "user1", "치즈냥", null, null, null, null, null, false, null, null
        );
        SummaryResult favorite = new SummaryResult("user1", "치즈냥", 42);
        HistoryResult history = new HistoryResult(
                1L, "user1", "치즈냥", 1, 42, null, null,
                "출석체크(+1)", false, 42, "출석체크(+1)", LocalDateTime.now()
        );

        given(authorizationPort.findById("user1")).willReturn(Optional.of(authorization));
        given(favoriteQueryPort.findById("user1")).willReturn(Optional.of(favorite));
        given(favoriteQueryPort.countHistory("user1")).willReturn(1L);
        given(favoriteQueryPort.countByFavoriteGreaterThan(42)).willReturn(3L);
        given(favoriteQueryPort.findHistory("user1", 50)).willReturn(List.of(history));

        // 실행
        FavoriteMeResult result = favoriteService.getMyFavorite("user1");

        // 검증
        then(result.userId()).isEqualTo("user1");
        then(result.nickName()).isEqualTo("치즈냥");
        then(result.favorite()).isEqualTo(42);
        then(result.rank()).isEqualTo(4);
        then(result.unseenCount()).isEqualTo(1L);
        then(result.histories()).hasSize(1);
        BDDMockito.then(authorizationPort).should(never())
                .markFavoriteHistorySeen(BDDMockito.anyString(), BDDMockito.any(LocalDateTime.class));
    }

    @Test
    void acknowledgeHistory_ShouldMarkFavoriteHistorySeen() {
        // 실행
        favoriteService.acknowledgeHistory("user1");

        // 검증
        BDDMockito.then(authorizationPort).should()
                .markFavoriteHistorySeen(BDDMockito.eq("user1"), BDDMockito.any(LocalDateTime.class));
    }
}
