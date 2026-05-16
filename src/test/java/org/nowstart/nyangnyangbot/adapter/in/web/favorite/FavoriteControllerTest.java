package org.nowstart.nyangnyangbot.adapter.in.web.favorite;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.in.web.weeklychat.response.WeeklyChatRankResponse;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase.WeeklyChatRankView;
import org.nowstart.nyangnyangbot.application.service.favorite.FavoriteService;
import org.nowstart.nyangnyangbot.application.service.weeklychat.WeeklyChatRankService;
import org.nowstart.nyangnyangbot.application.port.in.favorite.QueryFavoriteUseCase.FavoriteSummaryResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.servlet.ModelAndView;

@ExtendWith(MockitoExtension.class)
class FavoriteControllerTest {

    @Mock
    private FavoriteService favoriteService;

    @Mock
    private WeeklyChatRankService weeklyChatRankService;

    @InjectMocks
    private FavoriteController favoriteController;

    private List<FavoriteSummaryResult> favoriteEntities;
    private List<WeeklyChatRankView> weeklyChatRanks;
    private List<WeeklyChatRankResponse> weeklyChatRankResponses;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);

        favoriteEntities = List.of(
                new FavoriteSummaryResult("user1", "유저1", 100),
                new FavoriteSummaryResult("user2", "유저2", 50)
        );
        weeklyChatRanks = List.of(
                new WeeklyChatRankView(1, "채터1", 11L),
                new WeeklyChatRankView(2, "채터2", 7L)
        );
        weeklyChatRankResponses = weeklyChatRanks.stream()
                .map(WeeklyChatRankResponse::from)
                .toList();
        given(weeklyChatRankService.getWeeklyRanks(5)).willReturn(weeklyChatRanks);
    }

    @Test
    void favoriteList_ShouldReturnAllFavorites_WhenNickNameIsNull() {
        // 준비
        Page<FavoriteSummaryResult> expectedPage = new PageImpl<>(favoriteEntities, pageable, favoriteEntities.size());
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // 실행
        ModelAndView result = favoriteController.favoriteList(pageable, null, null);

        // 검증
        then(result.getViewName()).isEqualTo("index");
        then(result.getModel().get("landingMode")).isEqualTo(false);
        then(result.getModel().get("favoriteList")).isEqualTo(expectedPage);
        then(result.getModel().get("weeklyChatRanks")).isEqualTo(weeklyChatRankResponses);
        BDDMockito.then(weeklyChatRankService).should().getWeeklyRanks(5);
        BDDMockito.then(favoriteService).should().getList(any(Pageable.class));
        BDDMockito.then(favoriteService).should(never()).getByNickName(any(), anyString());
    }

    @Test
    void favoriteList_ShouldReturnFilteredFavorites_WhenNickNameProvided() {
        // 준비
        String nickName = "유저1";
        List<FavoriteSummaryResult> filteredList = Collections.singletonList(favoriteEntities.get(0));
        Page<FavoriteSummaryResult> expectedPage = new PageImpl<>(filteredList, pageable, filteredList.size());

        given(favoriteService.getByNickName(any(Pageable.class), eq(nickName))).willReturn(expectedPage);

        // 실행
        ModelAndView result = favoriteController.favoriteList(pageable, nickName, null);

        // 검증
        then(result.getViewName()).isEqualTo("index");
        then(result.getModel().get("favoriteList")).isEqualTo(expectedPage);
        BDDMockito.then(favoriteService).should().getByNickName(any(Pageable.class), eq(nickName));
        BDDMockito.then(favoriteService).should(never()).getList(any());
    }

    @Test
    void favoriteList_ShouldReturnAllFavorites_WhenNickNameIsEmpty() {
        // 준비
        Page<FavoriteSummaryResult> expectedPage = new PageImpl<>(favoriteEntities, pageable, favoriteEntities.size());
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // 실행
        ModelAndView result = favoriteController.favoriteList(pageable, "", null);

        // 검증
        then(result.getViewName()).isEqualTo("index");
        BDDMockito.then(favoriteService).should().getList(any(Pageable.class));
        BDDMockito.then(favoriteService).should(never()).getByNickName(any(), anyString());
    }

    @Test
    void favoriteList_ShouldReturnAllFavorites_WhenNickNameIsBlank() {
        // 준비
        Page<FavoriteSummaryResult> expectedPage = new PageImpl<>(favoriteEntities, pageable, favoriteEntities.size());
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // 실행
        ModelAndView result = favoriteController.favoriteList(pageable, "   ", null);

        // 검증
        then(result.getViewName()).isEqualTo("index");
        BDDMockito.then(favoriteService).should().getList(any(Pageable.class));
    }

    @Test
    void favoriteList_ShouldEscapeHtml_InNickName() {
        // 준비
        String maliciousNickName = "<script>alert('xss')</script>";
        String escapedNickName = "&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;";

        Page<FavoriteSummaryResult> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteService.getByNickName(any(Pageable.class), eq(escapedNickName))).willReturn(emptyPage);

        // 실행
        favoriteController.favoriteList(pageable, maliciousNickName, null);

        // 검증
        BDDMockito.then(favoriteService).should().getByNickName(any(Pageable.class), eq(escapedNickName));
    }

    @Test
    void favoriteList_ShouldApplyDescendingSort_ByFavorite() {
        // 준비
        Page<FavoriteSummaryResult> expectedPage = new PageImpl<>(favoriteEntities, pageable, favoriteEntities.size());
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // 실행
        favoriteController.favoriteList(pageable, null, null);

        // 검증
        BDDMockito.then(favoriteService).should().getList(argThat(p ->
                p.getSort().equals(Sort.by("favorite").descending())
        ));
    }

    @Test
    void favoriteList_ShouldPreservePageNumber() {
        // 준비
        Pageable page2 = PageRequest.of(2, 10);
        Page<FavoriteSummaryResult> expectedPage = new PageImpl<>(favoriteEntities, page2, 100);
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // 실행
        favoriteController.favoriteList(page2, null, null);

        // 검증
        BDDMockito.then(favoriteService).should().getList(argThat(p -> p.getPageNumber() == 2));
    }

    @Test
    void favoriteList_ShouldPreservePageSize() {
        // 준비
        Pageable customPageable = PageRequest.of(0, 50);
        Page<FavoriteSummaryResult> expectedPage = new PageImpl<>(favoriteEntities, customPageable, favoriteEntities.size());
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // 실행
        favoriteController.favoriteList(customPageable, null, null);

        // 검증
        BDDMockito.then(favoriteService).should().getList(argThat(p -> p.getPageSize() == 50));
    }

    @Test
    void favoriteList_ShouldHandleSpecialCharactersInNickName() {
        // 준비
        String specialNickName = "유저@#$%^&*()";
        Page<FavoriteSummaryResult> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteService.getByNickName(any(Pageable.class), anyString())).willReturn(emptyPage);

        // 실행
        ModelAndView result = favoriteController.favoriteList(pageable, specialNickName, null);

        // 검증
        then(result.getViewName()).isEqualTo("index");
        BDDMockito.then(favoriteService).should().getByNickName(any(Pageable.class), anyString());
    }

    @Test
    void favoriteList_ShouldReturnEmptyPage_WhenNoResults() {
        // 준비
        Page<FavoriteSummaryResult> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteService.getList(any(Pageable.class))).willReturn(emptyPage);

        // 실행
        ModelAndView result = favoriteController.favoriteList(pageable, null, null);

        // 검증
        then(result.getViewName()).isEqualTo("index");
        Page<FavoriteSummaryResult> resultPage = (Page<FavoriteSummaryResult>) result.getModel().get("favoriteList");
        then(resultPage.getContent()).isEmpty();
    }
}
