package org.nowstart.nyangnyangbot.adapter.in.web.favorite;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.favorite.QueryFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.favorite.QueryFavoriteUseCase.FavoriteMeResult;
import org.nowstart.nyangnyangbot.application.port.in.favorite.QueryFavoriteUseCase.FavoriteSummaryResult;
import org.nowstart.nyangnyangbot.application.port.in.upbo.QueryUpboUseCase;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase.WeeklyChatRankView;
import org.nowstart.nyangnyangbot.application.service.weeklychat.WeeklyChatRankService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.servlet.ModelAndView;

@ExtendWith(MockitoExtension.class)
class FavoriteControllerTest {

    @Mock
    private QueryFavoriteUseCase favoriteService;

    @Mock
    private QueryUpboUseCase queryUpboUseCase;

    @Mock
    private WeeklyChatRankService weeklyChatRankService;

    @InjectMocks
    private FavoriteController favoriteController;

    private List<FavoriteSummaryResult> favoriteEntities;
    private List<WeeklyChatRankView> weeklyChatRanks;
    private List<FavoriteController.WeeklyChatRankView> weeklyChatRankViews;
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
        weeklyChatRankViews = weeklyChatRanks.stream()
                .map(FavoriteController.WeeklyChatRankView::from)
                .toList();
        lenient().when(weeklyChatRankService.getWeeklyRanks(10)).thenReturn(weeklyChatRanks);
    }

    @Test
    @DisplayName("닉네임이 null이면 전체 즐겨찾기 목록을 반환한다")
    void favoriteList_ShouldReturnAllFavorites_WhenNickNameIsNull() {
        // 준비
        Page<FavoriteSummaryResult> expectedPage = new PageImpl<>(favoriteEntities, pageable, favoriteEntities.size());
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // 실행
        ModelAndView result = favoriteController.favoriteList(pageable, null, request(), response(), null);

        // 검증
        then(result.getViewName()).isEqualTo("index");
        then(result.getModel().get("landingMode")).isEqualTo(false);
        then(result.getModel().get("favoriteList")).isEqualTo(expectedPage);
        then(result.getModel().get("weeklyChatRanks")).isEqualTo(weeklyChatRankViews);
        BDDMockito.then(weeklyChatRankService).should().getWeeklyRanks(10);
        BDDMockito.then(favoriteService).should().getList(any(Pageable.class));
        BDDMockito.then(favoriteService).should(never()).getByNickName(any(), anyString());
    }

    @Test
    @DisplayName("닉네임이 제공되면 필터링된 즐겨찾기 목록을 반환한다")
    void favoriteList_ShouldReturnFilteredFavorites_WhenNickNameProvided() {
        // 준비
        String nickName = "유저1";
        List<FavoriteSummaryResult> filteredList = Collections.singletonList(favoriteEntities.get(0));
        Page<FavoriteSummaryResult> expectedPage = new PageImpl<>(filteredList, pageable, filteredList.size());

        given(favoriteService.getByNickName(any(Pageable.class), eq(nickName))).willReturn(expectedPage);

        // 실행
        ModelAndView result = favoriteController.favoriteList(pageable, nickName, request(), response(), adminAuthentication());

        // 검증
        then(result.getViewName()).isEqualTo("index");
        then(result.getModel().get("favoriteList")).isEqualTo(expectedPage);
        BDDMockito.then(favoriteService).should().getByNickName(any(Pageable.class), eq(nickName));
        BDDMockito.then(favoriteService).should(never()).getList(any());
    }

    @Test
    @DisplayName("닉네임이 빈 문자열이면 전체 즐겨찾기 목록을 반환한다")
    void favoriteList_ShouldReturnAllFavorites_WhenNickNameIsEmpty() {
        // 준비
        Page<FavoriteSummaryResult> expectedPage = new PageImpl<>(favoriteEntities, pageable, favoriteEntities.size());
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // 실행
        ModelAndView result = favoriteController.favoriteList(pageable, "", request(), response(), null);

        // 검증
        then(result.getViewName()).isEqualTo("index");
        BDDMockito.then(favoriteService).should().getList(any(Pageable.class));
        BDDMockito.then(favoriteService).should(never()).getByNickName(any(), anyString());
    }

    @Test
    @DisplayName("닉네임이 공백 문자열이면 전체 즐겨찾기 목록을 반환한다")
    void favoriteList_ShouldReturnAllFavorites_WhenNickNameIsBlank() {
        // 준비
        Page<FavoriteSummaryResult> expectedPage = new PageImpl<>(favoriteEntities, pageable, favoriteEntities.size());
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // 실행
        ModelAndView result = favoriteController.favoriteList(pageable, "   ", request(), response(), null);

        // 검증
        then(result.getViewName()).isEqualTo("index");
        BDDMockito.then(favoriteService).should().getList(any(Pageable.class));
    }

    @Test
    @DisplayName("HTML 문자를 포함한 닉네임도 검색 조건으로 그대로 전달한다")
    void favoriteList_ShouldPassHtmlLikeNickNameThrough() {
        // 준비
        String htmlLikeNickName = "<치즈냥>";

        Page<FavoriteSummaryResult> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteService.getByNickName(any(Pageable.class), eq(htmlLikeNickName))).willReturn(emptyPage);

        // 실행
        favoriteController.favoriteList(pageable, htmlLikeNickName, request(), response(), adminAuthentication());

        // 검증
        BDDMockito.then(favoriteService).should().getByNickName(any(Pageable.class), eq(htmlLikeNickName));
    }

    @Test
    @DisplayName("즐겨찾기 기준 내림차순 정렬을 적용한다")
    void favoriteList_ShouldApplyDescendingSort_ByFavorite() {
        // 준비
        Page<FavoriteSummaryResult> expectedPage = new PageImpl<>(favoriteEntities, pageable, favoriteEntities.size());
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // 실행
        favoriteController.favoriteList(pageable, null, request(), response(), null);

        // 검증
        BDDMockito.then(favoriteService).should().getList(argThat(p ->
                p.getSort().equals(Sort.by("favorite").descending())
        ));
    }

    @Test
    @DisplayName("페이지 번호를 유지한다")
    void favoriteList_ShouldPreservePageNumber() {
        // 준비
        Pageable page2 = PageRequest.of(2, 10);
        Page<FavoriteSummaryResult> expectedPage = new PageImpl<>(favoriteEntities, page2, 100);
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // 실행
        favoriteController.favoriteList(page2, null, request(), response(), null);

        // 검증
        BDDMockito.then(favoriteService).should().getList(argThat(p -> p.getPageNumber() == 2));
    }

    @Test
    @DisplayName("페이지 크기는 최대 50으로 제한한다")
    void favoriteList_ShouldClampPageSizeTo50() {
        // 준비
        Pageable requestedPageable = PageRequest.of(0, 500);
        Page<FavoriteSummaryResult> expectedPage = new PageImpl<>(favoriteEntities, PageRequest.of(0, 50), favoriteEntities.size());
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // 실행
        favoriteController.favoriteList(requestedPageable, null, request(), response(), null);

        // 검증
        BDDMockito.then(favoriteService).should().getList(argThat(p -> p.getPageSize() == 50));
    }

    @Test
    @DisplayName("닉네임에 특수문자가 포함된 경우에도 정상 처리한다")
    void favoriteList_ShouldHandleSpecialCharactersInNickName() {
        // 준비
        String specialNickName = "유저@#$%^&*()";
        Page<FavoriteSummaryResult> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteService.getByNickName(any(Pageable.class), anyString())).willReturn(emptyPage);

        // 실행
        ModelAndView result = favoriteController.favoriteList(pageable, specialNickName, request(), response(), adminAuthentication());

        // 검증
        then(result.getViewName()).isEqualTo("index");
        BDDMockito.then(favoriteService).should().getByNickName(any(Pageable.class), anyString());
    }

    @Test
    @DisplayName("관리자가 아니면 닉네임 검색 파라미터를 무시한다")
    void favoriteList_ShouldIgnoreNickNameSearch_WhenNotAdmin() {
        // 준비
        givenMyFavorite("user1", "유저1", 100);

        // 실행
        ModelAndView result = favoriteController.favoriteList(
                pageable,
                "유저1",
                request(),
                response(),
                userAuthentication()
        );

        // 검증
        then(result.getModel().get("nickName")).isEqualTo("");
        then(result.getModel().get("currentUserRank")).isEqualTo(7);
        Page<FavoriteSummaryResult> resultPage = (Page<FavoriteSummaryResult>) result.getModel().get("favoriteList");
        then(resultPage.getContent()).containsExactly(new FavoriteSummaryResult("user1", "유저1", 100));
        then(resultPage.getTotalElements()).isEqualTo(1);
        BDDMockito.then(favoriteService).should().getMyFavorite("user1");
        BDDMockito.then(favoriteService).should(never()).getList(any(Pageable.class));
        BDDMockito.then(favoriteService).should(never()).getByNickName(any(), anyString());
        BDDMockito.then(weeklyChatRankService).should().getWeeklyRanks(10);
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 페이지를 반환한다")
    void favoriteList_ShouldReturnEmptyPage_WhenNoResults() {
        // 준비
        Page<FavoriteSummaryResult> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteService.getList(any(Pageable.class))).willReturn(emptyPage);

        // 실행
        ModelAndView result = favoriteController.favoriteList(pageable, null, request(), response(), null);

        // 검증
        then(result.getViewName()).isEqualTo("index");
        Page<FavoriteSummaryResult> resultPage = (Page<FavoriteSummaryResult>) result.getModel().get("favoriteList");
        then(resultPage.getContent()).isEmpty();
    }

    @Test
    @DisplayName("htmx 호감도 목록 요청은 board fragment와 full page URL push 헤더를 반환한다")
    void favoriteList_ShouldReturnBoardRegionAndPushFullPageUrl_WhenHtmxRequest() {
        // 준비
        String nickName = "유저1";
        Page<FavoriteSummaryResult> expectedPage = new PageImpl<>(List.of(favoriteEntities.get(0)), pageable, 1);
        given(favoriteService.getByNickName(any(Pageable.class), eq(nickName))).willReturn(expectedPage);
        MockHttpServletRequest request = htmxRequest();
        request.setContextPath("/nyang-nyang-bot");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 실행
        ModelAndView result = favoriteController.favoriteList(pageable, nickName, request, response, adminAuthentication());

        // 검증
        then(result.getViewName()).isEqualTo("features/favorite/components :: favorite-board-region");
        then(result.getModel().get("favoriteList")).isEqualTo(expectedPage);
        then(result.getModel().get("nickName")).isEqualTo(nickName);
        then(response.getHeader("HX-Push-Url"))
                .isEqualTo("/nyang-nyang-bot/favorite/list?page=0&size=10&nickName=%EC%9C%A0%EC%A0%801");
    }

    @Test
    @DisplayName("관리자가 아닌 htmx 호감도 목록 요청도 검색 조건과 관리자 UI 모델을 제거한다")
    void favoriteList_ShouldReturnNonAdminBoardModel_WhenHtmxRequestIsNotAdmin() {
        // 준비
        givenMyFavorite("user1", "유저1", 100);
        MockHttpServletRequest request = htmxRequest();
        request.setContextPath("/nyang-nyang-bot");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 실행
        ModelAndView result = favoriteController.favoriteList(pageable, "유저1", request, response, userAuthentication());

        // 검증
        then(result.getViewName()).isEqualTo("features/favorite/components :: favorite-board-region");
        Page<FavoriteSummaryResult> resultPage = (Page<FavoriteSummaryResult>) result.getModel().get("favoriteList");
        then(resultPage.getContent()).containsExactly(new FavoriteSummaryResult("user1", "유저1", 100));
        then(result.getModel().get("nickName")).isEqualTo("");
        then(result.getModel().get("weeklyChatRanks")).isEqualTo(weeklyChatRankViews);
        then(result.getModel().get("isAdmin")).isEqualTo(false);
        then(result.getModel().get("currentUserId")).isEqualTo("user1");
        then(result.getModel().get("currentUserRank")).isEqualTo(7);
        then(response.getHeader("HX-Push-Url")).isEqualTo("/nyang-nyang-bot/favorite/list?page=0&size=10");
        BDDMockito.then(favoriteService).should().getMyFavorite("user1");
        BDDMockito.then(favoriteService).should(never()).getList(any(Pageable.class));
        BDDMockito.then(favoriteService).should(never()).getByNickName(any(), anyString());
    }

    private void givenMyFavorite(String userId, String nickName, Integer favorite) {
        given(favoriteService.getMyFavorite(userId))
                .willReturn(new FavoriteMeResult(userId, nickName, favorite, 7, List.of()));
        given(queryUpboUseCase.getUserUpbos(userId, null)).willReturn(List.of());
    }

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest();
    }

    private MockHttpServletRequest htmxRequest() {
        MockHttpServletRequest request = request();
        request.addHeader("HX-Request", "true");
        return request;
    }

    private MockHttpServletResponse response() {
        return new MockHttpServletResponse();
    }

    private Authentication adminAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "admin",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    private Authentication userAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "user1",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
