package org.nowstart.nyangnyangbot.adapter.in.web.favorite;

import io.micrometer.common.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.adapter.in.web.favorite.response.FavoriteHistoryResponse;
import org.nowstart.nyangnyangbot.adapter.in.web.favorite.response.FavoriteMeResponse;
import org.nowstart.nyangnyangbot.adapter.in.web.favorite.response.WeeklyChatRankResponse;
import org.nowstart.nyangnyangbot.application.port.in.favorite.QueryFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/favorite")
@Tag(name = "Favorite API", description = "호감도 관련 API")
public class FavoriteController {

    private static final String FAVORITE_LIST_VIEW = "index";
    private static final int MAX_HISTORY_LIMIT = 50;
    private static final int WEEKLY_CHAT_RANK_LIMIT = 5;

    private final QueryFavoriteUseCase queryFavoriteUseCase;
    private final QueryWeeklyChatRankUseCase queryWeeklyChatRankUseCase;

    @Operation(
            summary = "즐겨찾기 리스트 조회",
            description = "닉네임으로 필터링 가능하며, 페이지네이션과 정렬을 지원합니다."
    )
    @GetMapping("/list")
    public ModelAndView favoriteList(
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) String nickName,
            Authentication authentication
    ) {
        log.info("[GET][/favorite/list]");
        String safeNickName = Optional.ofNullable(nickName).map(HtmlUtils::htmlEscape).orElse("");
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("favorite").descending());
        var favoriteList =
                StringUtils.isBlank(safeNickName) ? queryFavoriteUseCase.getList(page) : queryFavoriteUseCase.getByNickName(page, safeNickName);

        ModelAndView modelAndView = new ModelAndView(FAVORITE_LIST_VIEW, "favoriteList", favoriteList);
        modelAndView.addObject("landingMode", false);
        modelAndView.addObject("weeklyChatRanks", queryWeeklyChatRankUseCase.getWeeklyRanks(WEEKLY_CHAT_RANK_LIMIT).stream()
                .map(WeeklyChatRankResponse::from)
                .toList());
        boolean isAdmin = false;
        String currentUserId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            currentUserId = authentication.getName();
            isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
            queryFavoriteUseCase.getCurrentNickName(authentication.getName())
                    .ifPresent(name -> modelAndView.addObject("currentNickName", name));
        }
        modelAndView.addObject("currentUserId", currentUserId);
        modelAndView.addObject("isAdmin", isAdmin);
        return modelAndView;
    }

    @Operation(summary = "호감도 히스토리 조회", description = "ADMIN은 전체 조회, 그 외는 본인 계정만 조회 가능합니다.")
    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
    public ResponseEntity<List<FavoriteHistoryResponse>> favoriteHistory(
            @RequestParam String userId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_HISTORY_LIMIT);
        List<FavoriteHistoryResponse> body = queryFavoriteUseCase.getHistory(userId, safeLimit).stream()
                .map(FavoriteHistoryResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "본인 호감도 요약 조회", description = "인증 사용자의 현재 호감도와 최근 히스토리를 조회하고 미확인 내역을 읽음 처리합니다.")
    @GetMapping("/me")
    public ResponseEntity<FavoriteMeResponse> favoriteMe(Authentication authentication) {
        return ResponseEntity.ok(FavoriteMeResponse.from(queryFavoriteUseCase.getMyFavorite(authentication.getName())));
    }
}
