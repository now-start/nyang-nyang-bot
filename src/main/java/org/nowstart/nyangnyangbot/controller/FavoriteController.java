package org.nowstart.nyangnyangbot.controller;

import io.micrometer.common.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.favorite.FavoriteHistoryDto;
import org.nowstart.nyangnyangbot.data.dto.favorite.FavoriteListItemDto;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.repository.AuthorizationRepository;
import org.nowstart.nyangnyangbot.service.FavoriteService;
import org.nowstart.nyangnyangbot.service.WeeklyChatRankService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

    private final FavoriteService favoriteService;
    private final AuthorizationRepository authorizationRepository;
    private final WeeklyChatRankService weeklyChatRankService;

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
        Pageable page = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(resolveFavoriteSortProperty()).descending()
        );
        Page<FavoriteEntity> favoriteList =
                StringUtils.isBlank(safeNickName) ? favoriteService.getList(page) : favoriteService.getByNickName(page, safeNickName);
        Page<FavoriteListItemDto> favoriteListView = toFavoriteListView(favoriteList);

        ModelAndView modelAndView = new ModelAndView(FAVORITE_LIST_VIEW, "favoriteList", favoriteList);
        modelAndView.addObject("favoriteListView", favoriteListView);
        modelAndView.addObject("landingMode", false);
        modelAndView.addObject("weeklyChatRanks", weeklyChatRankService.getWeeklyRanks(WEEKLY_CHAT_RANK_LIMIT));
        boolean isAdmin = false;
        String currentUserId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            currentUserId = authentication.getName();
            isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
            authorizationRepository.findById(authentication.getName())
                    .map(authorization -> authorization.getChannelName())
                    .filter(name -> !name.isBlank())
                    .ifPresent(name -> modelAndView.addObject("currentNickName", name));
        }
        modelAndView.addObject("currentUserId", currentUserId);
        modelAndView.addObject("isAdmin", isAdmin);
        return modelAndView;
    }

    @Operation(summary = "호감도 히스토리 조회", description = "ADMIN은 전체 조회, 그 외는 본인 계정만 조회 가능합니다.")
    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
    public ResponseEntity<List<FavoriteHistoryDto>> favoriteHistory(
            @RequestParam String userId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_HISTORY_LIMIT);
        List<FavoriteHistoryDto> body = favoriteService.getHistory(userId, safeLimit).stream()
                .map(FavoriteHistoryDto::from)
                .toList();
        return ResponseEntity.ok(body);
    }

    private Page<FavoriteListItemDto> toFavoriteListView(Page<FavoriteEntity> favoriteList) {
        List<FavoriteListItemDto> content = favoriteList.stream()
                .map(this::toFavoriteListItem)
                .toList();
        return new PageImpl<>(content, favoriteList.getPageable(), favoriteList.getTotalElements());
    }

    private FavoriteListItemDto toFavoriteListItem(FavoriteEntity favoriteEntity) {
        Integer legacyFavorite = readIntegerProperty(favoriteEntity, "favorite");
        Integer totalFavorite = readIntegerProperty(favoriteEntity, "totalFavorite");
        Integer displayFavorite = totalFavorite != null ? totalFavorite : legacyFavorite;
        if (displayFavorite == null) {
            displayFavorite = 0;
        }
        if (legacyFavorite == null) {
            legacyFavorite = displayFavorite;
        }
        if (totalFavorite == null) {
            totalFavorite = displayFavorite;
        }
        return new FavoriteListItemDto(
                favoriteEntity.getUserId(),
                favoriteEntity.getNickName(),
                legacyFavorite,
                totalFavorite
        );
    }

    private Integer readIntegerProperty(Object target, String propertyName) {
        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(target);
        if (!beanWrapper.isReadableProperty(propertyName)) {
            return null;
        }
        Object value = beanWrapper.getPropertyValue(propertyName);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveFavoriteSortProperty() {
        return BeanUtils.getPropertyDescriptor(FavoriteEntity.class, "totalFavorite") != null
                ? "totalFavorite"
                : "favorite";
    }
}
