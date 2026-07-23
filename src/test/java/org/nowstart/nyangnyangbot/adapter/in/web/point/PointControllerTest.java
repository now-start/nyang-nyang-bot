package org.nowstart.nyangnyangbot.adapter.in.web.point;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.point.QueryPointUseCase;
import org.nowstart.nyangnyangbot.application.port.in.point.QueryPointUseCase.PointHistoryResult;
import org.nowstart.nyangnyangbot.application.port.in.point.QueryPointUseCase.PointMeResult;
import org.nowstart.nyangnyangbot.application.port.in.point.QueryPointUseCase.PointSummaryResult;
import org.nowstart.nyangnyangbot.application.port.in.reward.QueryRewardUseCase;
import org.nowstart.nyangnyangbot.application.port.in.reward.QueryRewardUseCase.RewardResult;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase;
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
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.ModelAndView;

@ExtendWith(MockitoExtension.class)
class PointControllerTest {

    @Mock
    private QueryPointUseCase queryPointUseCase;

    @Mock
    private QueryRewardUseCase queryRewardUseCase;

    @Mock
    private QueryWeeklyChatRankUseCase queryWeeklyChatRankUseCase;

    private PointController controller;

    @BeforeEach
    void setUp() {
        controller = new PointController(queryPointUseCase, queryRewardUseCase, queryWeeklyChatRankUseCase);
    }

    @Test
    void pointList_ShouldUseCanonicalAdminSearchAndHtmxContract() {
        given(queryWeeklyChatRankUseCase.getWeeklyRanks(10)).willReturn(List.of());
        Page<PointSummaryResult> points = new PageImpl<>(
                List.of(new PointSummaryResult("user1", "유저1", 100)),
                PageRequest.of(0, 50),
                1
        );
        given(queryPointUseCase.getByDisplayName(any(Pageable.class), eq("유저1"))).willReturn(points);
        given(queryPointUseCase.getCurrentDisplayName("admin")).willReturn(java.util.Optional.of("관리자"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("HX-Request", "true");
        request.setContextPath("/nyang-nyang-bot");
        MockHttpServletResponse response = new MockHttpServletResponse();

        ModelAndView result = controller.pointList(
                PageRequest.of(0, 500),
                "유저1",
                request,
                response,
                adminAuthentication()
        );

        then(result.getViewName()).isEqualTo("features/point/components :: point-board-region");
        then(result.getModel().get("pointList")).isEqualTo(points);
        then(result.getModel().get("nickName")).isEqualTo("유저1");
        then(result.getModel().get("isAdmin")).isEqualTo(true);
        then(response.getHeader("HX-Push-Url"))
                .isEqualTo("/nyang-nyang-bot/points/list?page=0&size=50&nickName=%EC%9C%A0%EC%A0%801");
        BDDMockito.then(queryPointUseCase).should().getByDisplayName(argThat(pageable ->
                pageable.getPageSize() == 50
                        && pageable.getSort().equals(Sort.by("balance").descending())), eq("유저1"));
        BDDMockito.then(queryPointUseCase).should(never()).getList(any());
    }

    @Test
    void pointList_ShouldUseUnfilteredQueryForBlankAdminSearch() {
        Page<PointSummaryResult> points = new PageImpl<>(
                List.of(new PointSummaryResult("user1", "유저1", 100)),
                PageRequest.of(2, 10),
                30
        );
        given(queryWeeklyChatRankUseCase.getWeeklyRanks(10)).willReturn(List.of());
        given(queryPointUseCase.getList(any(Pageable.class))).willReturn(points);
        given(queryPointUseCase.getCurrentDisplayName("admin")).willReturn(java.util.Optional.of("관리자"));

        ModelAndView result = controller.pointList(
                PageRequest.of(2, 10),
                "   ",
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                adminAuthentication()
        );

        then(result.getViewName()).isEqualTo("index");
        then(result.getModel().get("pointList")).isEqualTo(points);
        BDDMockito.then(queryPointUseCase).should().getList(argThat(pageable ->
                pageable.getPageNumber() == 2
                        && pageable.getSort().equals(Sort.by("balance").descending())));
        BDDMockito.then(queryPointUseCase).should(never()).getByDisplayName(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void pointList_ShouldLimitRegularUserToOwnPointAndRewards() {
        given(queryWeeklyChatRankUseCase.getWeeklyRanks(10)).willReturn(List.of());
        given(queryPointUseCase.getMyPoint("user1"))
                .willReturn(new PointMeResult("user1", "유저1", 100, 7, List.of()));
        given(queryRewardUseCase.getUserRewards("user1", null, 20)).willReturn(List.of(
                new RewardResult(
                        3L,
                        "user1",
                        null,
                        9L,
                        "업보 차감권",
                        "COUPON",
                        "MANUAL",
                        null,
                        "OWNED",
                        "보유 보상",
                        "reward:user1:3",
                        Instant.parse("2026-07-09T10:30:00Z")
                )
        ));

        ModelAndView result = controller.pointList(
                PageRequest.of(2, 10),
                "무시할 검색어",
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                userAuthentication()
        );

        Page<PointSummaryResult> resultPage = (Page<PointSummaryResult>) result.getModel().get("pointList");
        then(result.getViewName()).isEqualTo("index");
        then(resultPage.getContent()).containsExactly(new PointSummaryResult("user1", "유저1", 100));
        then(result.getModel().get("nickName")).isEqualTo("");
        then(result.getModel().get("currentUserRank")).isEqualTo(7L);
        then((List<PointController.RewardView>) result.getModel().get("userRewards"))
                .singleElement()
                .satisfies(reward -> {
                    then(reward.id()).isEqualTo(3L);
                    then(reward.pointLedgerEntryId()).isEqualTo(9L);
                    then(reward.date()).isEqualTo("2026-07-09 19:30");
                });
        BDDMockito.then(queryPointUseCase).should(never()).getList(any());
        BDDMockito.then(queryPointUseCase).should(never()).getByDisplayName(any(), any());
        BDDMockito.then(queryRewardUseCase).should().getUserRewards("user1", null, 20);
    }

    @Test
    @SuppressWarnings("unchecked")
    void pointHistory_ShouldExposeCanonicalLedgerFieldsAndClampLimit() {
        given(queryPointUseCase.getHistory("user1", 50)).willReturn(List.of(
                new PointHistoryResult(
                        7L,
                        "user1",
                        5,
                        20,
                        "PRESENCE_REWARD",
                        "생존 확인 보상",
                        false,
                        Instant.parse("2026-03-22T05:30:00Z")
                )
        ));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.pointHistory("user1", 500, model);

        then(view).isEqualTo("features/point/components :: history-grid");
        List<PointController.PointHistoryView> histories =
                (List<PointController.PointHistoryView>) model.get("histories");
        then(histories).singleElement().satisfies(history -> {
            then(history.ledgerId()).isEqualTo(7L);
            then(history.userId()).isEqualTo("user1");
            then(history.delta()).isEqualTo(5L);
            then(history.balanceAfter()).isEqualTo(20L);
            then(history.sourceType()).isEqualTo("PRESENCE_REWARD");
            then(history.description()).isEqualTo("생존 확인 보상");
            then(history.correction()).isFalse();
            then(history.date()).isEqualTo("2026-03-22 14:30");
        });
    }

    private Authentication adminAuthentication() {
        return authentication("admin", "ROLE_ADMIN");
    }

    private Authentication userAuthentication() {
        return authentication("user1", "ROLE_USER");
    }

    private Authentication authentication(String name, String authority) {
        return new UsernamePasswordAuthenticationToken(
                name,
                "N/A",
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}
