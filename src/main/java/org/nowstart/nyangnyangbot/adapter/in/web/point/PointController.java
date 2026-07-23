package org.nowstart.nyangnyangbot.adapter.in.web.point;

import io.micrometer.common.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.point.QueryPointUseCase;
import org.nowstart.nyangnyangbot.application.port.in.point.QueryPointUseCase.PointHistoryResult;
import org.nowstart.nyangnyangbot.application.port.in.reward.QueryRewardUseCase;
import org.nowstart.nyangnyangbot.application.port.in.reward.QueryRewardUseCase.RewardResult;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/points")
@Tag(name = "Point API", description = "호감도 포인트 관련 API")
public class PointController {

    private static final String POINT_LIST_VIEW = "index";
    private static final String POINT_BOARD_FRAGMENT = "features/point/components :: point-board-region";
    private static final String POINT_HISTORY_FRAGMENT = "features/point/components :: history-grid";
    private static final int MAX_HISTORY_LIMIT = 50;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int USER_REWARD_LIMIT = 20;
    private static final int WEEKLY_CHAT_RANK_LIMIT = 10;
    private static final DateTimeFormatter HISTORY_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.of("Asia/Seoul"));

    private final QueryPointUseCase queryPointUseCase;
    private final QueryRewardUseCase queryRewardUseCase;
    private final QueryWeeklyChatRankUseCase queryWeeklyChatRankUseCase;
    @Value("${nyang.local-auth.login-page-enabled:false}")
    private boolean localTestLoginEnabled;

    @Operation(summary = "호감도 목록 조회", description = "닉네임 필터, 페이지네이션과 정렬을 지원합니다.")
    @GetMapping("/list")
    public ModelAndView pointList(
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) String nickName,
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        PointListModel listModel = pointListModel(pageable, nickName, authentication);
        String viewName = isHtmxRequest(request) ? POINT_BOARD_FRAGMENT : POINT_LIST_VIEW;
        ModelAndView modelAndView = new ModelAndView(viewName, "pointList", listModel.pointList());
        addPointListModel(modelAndView, listModel);
        if (isHtmxRequest(request)) {
            response.setHeader("HX-Push-Url", pointListUrl(request, listModel.page(), listModel.nickName()));
        }
        return modelAndView;
    }

    @Operation(summary = "호감도 이력 fragment 조회", description = "ADMIN은 전체 조회, 그 외는 본인 계정만 조회 가능합니다.")
    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
    public String pointHistory(
            @RequestParam String userId,
            @RequestParam(defaultValue = "10") int limit,
            Model model
    ) {
        model.addAttribute("histories", pointHistories(userId, limit));
        return POINT_HISTORY_FRAGMENT;
    }

    private PointListModel pointListModel(Pageable pageable, String nickName, Authentication authentication) {
        boolean admin = isAdmin(authentication);
        String currentUserId = currentUserId(authentication);
        int pageSize = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        Pageable page = PageRequest.of(Math.max(pageable.getPageNumber(), 0), pageSize,
                Sort.by("balance").descending());
        if (!admin && currentUserId != null) {
            var myPoint = queryPointUseCase.getMyPoint(currentUserId);
            var pointList = new PageImpl<>(
                    List.of(new QueryPointUseCase.PointSummaryResult(
                            myPoint.userId(),
                            myPoint.displayName(),
                            myPoint.point()
                    )),
                    PageRequest.of(0, pageSize, Sort.by("balance").descending()),
                    1
            );
            return new PointListModel(pointList, PageRequest.of(0, pageSize), "", weeklyChatRanks(),
                    currentUserId, myPoint.displayName(), false, myPoint.rank(), rewards(currentUserId));
        }

        String searchDisplayName = admin && nickName != null ? nickName : "";
        var pointList = StringUtils.isBlank(searchDisplayName)
                ? queryPointUseCase.getList(page)
                : queryPointUseCase.getByDisplayName(page, searchDisplayName);
        String currentDisplayName = currentUserId == null
                ? null
                : queryPointUseCase.getCurrentDisplayName(currentUserId).orElse(null);
        return new PointListModel(pointList, page, searchDisplayName, weeklyChatRanks(),
                currentUserId, currentDisplayName, admin, null, List.of());
    }

    private void addPointListModel(ModelAndView modelAndView, PointListModel model) {
        modelAndView.addObject("landingMode", false);
        modelAndView.addObject("nickName", model.nickName());
        modelAndView.addObject("weeklyChatRanks", model.weeklyChatRanks());
        modelAndView.addObject("currentUserId", model.currentUserId());
        modelAndView.addObject("currentNickName", model.currentDisplayName());
        modelAndView.addObject("isAdmin", model.admin());
        modelAndView.addObject("currentUserRank", model.currentUserRank());
        modelAndView.addObject("userRewards", model.userRewards());
        modelAndView.addObject("localTestLoginEnabled", localTestLoginEnabled);
    }

    private List<RewardView> rewards(String userId) {
        return queryRewardUseCase.getUserRewards(userId, null, USER_REWARD_LIMIT).stream()
                .map(RewardView::from)
                .toList();
    }

    private List<WeeklyChatRankView> weeklyChatRanks() {
        return queryWeeklyChatRankUseCase.getWeeklyRanks(WEEKLY_CHAT_RANK_LIMIT).stream()
                .map(WeeklyChatRankView::from)
                .toList();
    }

    private List<PointHistoryView> pointHistories(String userId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_HISTORY_LIMIT);
        return queryPointUseCase.getHistory(userId, safeLimit).stream()
                .map(PointHistoryView::from)
                .toList();
    }

    private String pointListUrl(HttpServletRequest request, Pageable page, String nickName) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(request.getContextPath() + "/points/list")
                .queryParam("page", page.getPageNumber())
                .queryParam("size", page.getPageSize());
        if (!StringUtils.isBlank(nickName)) {
            builder.queryParam("nickName", nickName);
        }
        return builder.build().encode().toUriString();
    }

    private boolean isHtmxRequest(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader("HX-Request"));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private String currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }

    private record PointListModel(
            org.springframework.data.domain.Page<QueryPointUseCase.PointSummaryResult> pointList,
            Pageable page,
            String nickName,
            List<WeeklyChatRankView> weeklyChatRanks,
            String currentUserId,
            String currentDisplayName,
            boolean admin,
            Long currentUserRank,
            List<RewardView> userRewards
    ) {
    }

    public record WeeklyChatRankView(Integer rank, String nickname, Long chatCount) {

        static WeeklyChatRankView from(QueryWeeklyChatRankUseCase.WeeklyChatRankView view) {
            return new WeeklyChatRankView(view.rank(), view.nickname(), view.chatCount());
        }
    }

    public record PointHistoryView(
            long ledgerId,
            String userId,
            long delta,
            long balanceAfter,
            String sourceType,
            String description,
            boolean correction,
            String date
    ) {

        static PointHistoryView from(PointHistoryResult result) {
            return new PointHistoryView(
                    result.ledgerId(),
                    result.userId(),
                    result.delta(),
                    result.balanceAfter(),
                    result.sourceType(),
                    result.description(),
                    result.correction(),
                    result.createdAt() == null ? null : HISTORY_DATE_FORMATTER.format(result.createdAt())
            );
        }
    }

    public record RewardView(
            Long id,
            String label,
            String status,
            Long pointDelta,
            String rewardType,
            String conversionMode,
            Long pointLedgerEntryId,
            String description,
            String date
    ) {

        static RewardView from(RewardResult result) {
            return new RewardView(
                    result.id(),
                    result.label(),
                    result.status(),
                    result.pointDelta(),
                    result.rewardType(),
                    result.conversionMode(),
                    result.pointLedgerEntryId(),
                    result.description(),
                    result.createdAt() == null ? null : HISTORY_DATE_FORMATTER.format(result.createdAt())
            );
        }
    }
}
