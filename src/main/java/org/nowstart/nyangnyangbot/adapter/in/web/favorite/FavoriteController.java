package org.nowstart.nyangnyangbot.adapter.in.web.favorite;

import io.micrometer.common.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AcknowledgeFavoriteHistoryUseCase;
import org.nowstart.nyangnyangbot.application.port.in.favorite.QueryFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.favorite.QueryFavoriteUseCase.FavoriteHistoryResult;
import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase.UserUpboResult;
import org.nowstart.nyangnyangbot.application.port.in.upbo.QueryUpboUseCase;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/favorite")
@Tag(name = "Favorite API", description = "호감도 관련 API")
public class FavoriteController {

    private static final String FAVORITE_LIST_VIEW = "index";
    private static final String FAVORITE_BOARD_FRAGMENT = "features/favorite/components :: favorite-board-region";
    private static final String FAVORITE_HISTORY_FRAGMENT = "features/favorite/components :: history-grid";
    private static final int MAX_HISTORY_LIMIT = 50;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int WEEKLY_CHAT_RANK_LIMIT = 10;
    private static final DateTimeFormatter HISTORY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final QueryFavoriteUseCase queryFavoriteUseCase;
    private final AcknowledgeFavoriteHistoryUseCase acknowledgeFavoriteHistoryUseCase;
    private final QueryUpboUseCase queryUpboUseCase;
    private final QueryWeeklyChatRankUseCase queryWeeklyChatRankUseCase;
    @Value("${nyang.local-auth.login-page-enabled:false}")
    private boolean localTestLoginEnabled;

    @Operation(
            summary = "즐겨찾기 리스트 조회",
            description = "닉네임으로 필터링 가능하며, 페이지네이션과 정렬을 지원합니다."
    )
    @GetMapping("/list")
    public ModelAndView favoriteList(
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) String nickName,
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        log.info("[GET][/favorite/list]");
        FavoriteListModel favoriteListModel = favoriteListModel(pageable, nickName, authentication);
        String viewName = isHtmxRequest(request) ? FAVORITE_BOARD_FRAGMENT : FAVORITE_LIST_VIEW;
        ModelAndView modelAndView = new ModelAndView(viewName, "favoriteList", favoriteListModel.favoriteList());
        addFavoriteListModel(modelAndView, favoriteListModel);
        if (isHtmxRequest(request)) {
            response.setHeader("HX-Push-Url", favoriteListUrl(request, favoriteListModel.page(), favoriteListModel.nickName()));
        }
        return modelAndView;
    }

    @Operation(summary = "호감도 히스토리 fragment 조회", description = "ADMIN은 전체 조회, 그 외는 본인 계정만 조회 가능합니다.")
    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
    public String favoriteHistory(
            @RequestParam String userId,
            @RequestParam(defaultValue = "10") int limit,
            Model model
    ) {
        model.addAttribute("histories", favoriteHistories(userId, limit));
        return FAVORITE_HISTORY_FRAGMENT;
    }

    @Operation(summary = "호감도 히스토리 확인", description = "히스토리를 조회하고 본인 계정의 미확인 상태를 갱신합니다.")
    @PostMapping("/history/acknowledge")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
    public String acknowledgeFavoriteHistory(
            @RequestParam String userId,
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication,
            Model model
    ) {
        model.addAttribute("histories", favoriteHistories(userId, limit));
        if (authentication != null && userId.equals(authentication.getName())) {
            acknowledgeFavoriteHistoryUseCase.acknowledgeHistory(userId);
        }
        return FAVORITE_HISTORY_FRAGMENT;
    }

    private FavoriteListModel favoriteListModel(Pageable pageable, String nickName, Authentication authentication) {
        boolean isAdmin = isAdmin(authentication);
        String currentUserId = currentUserId(authentication);
        int pageSize = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageSize, Sort.by("favorite").descending());
        if (!isAdmin && currentUserId != null) {
            var myFavorite = queryFavoriteUseCase.getMyFavorite(currentUserId);
            var favoriteList = new PageImpl<>(
                    List.of(new QueryFavoriteUseCase.FavoriteSummaryResult(
                            myFavorite.userId(),
                            myFavorite.nickName(),
                            myFavorite.favorite()
                    )),
                    PageRequest.of(0, pageSize, Sort.by("favorite").descending()),
                    1
            );
            return new FavoriteListModel(favoriteList, PageRequest.of(0, pageSize), "", weeklyChatRanks(),
                    currentUserId, myFavorite.nickName(), false, myFavorite.rank(), userUpbos(currentUserId));
        }

        String searchNickName = isAdmin && nickName != null ? nickName : "";
        var favoriteList =
                StringUtils.isBlank(searchNickName) ? queryFavoriteUseCase.getList(page) : queryFavoriteUseCase.getByNickName(page, searchNickName);
        String currentNickName = currentUserId == null
                ? null
                : queryFavoriteUseCase.getCurrentNickName(currentUserId).orElse(null);
        return new FavoriteListModel(favoriteList, page, searchNickName, weeklyChatRanks(),
                currentUserId, currentNickName, isAdmin, null, List.of());
    }

    private void addFavoriteListModel(ModelAndView modelAndView, FavoriteListModel favoriteListModel) {
        modelAndView.addObject("landingMode", false);
        modelAndView.addObject("nickName", favoriteListModel.nickName());
        modelAndView.addObject("weeklyChatRanks", favoriteListModel.weeklyChatRanks());
        modelAndView.addObject("currentUserId", favoriteListModel.currentUserId());
        modelAndView.addObject("currentNickName", favoriteListModel.currentNickName());
        modelAndView.addObject("isAdmin", favoriteListModel.isAdmin());
        modelAndView.addObject("currentUserRank", favoriteListModel.currentUserRank());
        modelAndView.addObject("userUpbos", favoriteListModel.userUpbos());
        modelAndView.addObject("localTestLoginEnabled", localTestLoginEnabled);
    }

    private List<UserUpboView> userUpbos(String userId) {
        return queryUpboUseCase.getUserUpbos(userId, null).stream()
                .map(UserUpboView::from)
                .toList();
    }

    private List<WeeklyChatRankView> weeklyChatRanks() {
        return queryWeeklyChatRankUseCase.getWeeklyRanks(WEEKLY_CHAT_RANK_LIMIT).stream()
                .map(WeeklyChatRankView::from)
                .toList();
    }

    private List<FavoriteHistoryView> favoriteHistories(String userId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_HISTORY_LIMIT);
        return queryFavoriteUseCase.getHistory(userId, safeLimit).stream()
                .map(FavoriteHistoryView::from)
                .toList();
    }

    private String favoriteListUrl(HttpServletRequest request, Pageable page, String nickName) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(request.getContextPath() + "/favorite/list")
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

    private record FavoriteListModel(
            org.springframework.data.domain.Page<QueryFavoriteUseCase.FavoriteSummaryResult> favoriteList,
            Pageable page,
            String nickName,
            List<WeeklyChatRankView> weeklyChatRanks,
            String currentUserId,
            String currentNickName,
            boolean isAdmin,
            Integer currentUserRank,
            List<UserUpboView> userUpbos
    ) {
    }

    public record WeeklyChatRankView(
            Integer rank,
            String nickname,
            Long chatCount
    ) {

        static WeeklyChatRankView from(QueryWeeklyChatRankUseCase.WeeklyChatRankView view) {
            return new WeeklyChatRankView(view.rank(), view.nickname(), view.chatCount());
        }
    }

    public record FavoriteHistoryView(
            Long ledgerId,
            String channelId,
            String nickNameSnapshot,
            Integer delta,
            Integer balanceAfter,
            String sourceType,
            String displayCategory,
            String publicDescription,
            Boolean correction,
            Integer favorite,
            String history,
            String date
    ) {

        static FavoriteHistoryView from(FavoriteHistoryResult result) {
            String formattedDate = result.createdAt() == null ? null : result.createdAt().format(HISTORY_DATE_FORMATTER);
            return new FavoriteHistoryView(
                    result.ledgerId(),
                    result.channelId(),
                    result.nickNameSnapshot(),
                    result.delta(),
                    result.balanceAfter(),
                    result.sourceType(),
                    result.displayCategory(),
                    result.publicDescription(),
                    result.correction(),
                    result.favorite(),
                    result.history(),
                    formattedDate
            );
        }
    }

    public record UserUpboView(
            Long id,
            String label,
            String status,
            Integer exchangeFavoriteValue,
            String rewardType,
            String conversionMode,
            Long ledgerId,
            String publicDescription,
            String date
    ) {

        static UserUpboView from(UserUpboResult result) {
            String formattedDate = result.createdAt() == null ? null : result.createdAt().format(HISTORY_DATE_FORMATTER);
            return new UserUpboView(
                    result.id(),
                    result.label(),
                    result.status(),
                    result.exchangeFavoriteValue(),
                    result.rewardType(),
                    result.conversionMode(),
                    result.ledgerId(),
                    result.publicDescription(),
                    formattedDate
            );
        }
    }
}
