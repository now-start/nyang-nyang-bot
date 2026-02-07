package org.nowstart.nyangnyangbot.controller;

import io.micrometer.common.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.FavoriteHistoryResponse;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.service.FavoriteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/favorite")
@Tag(name = "Favorite API", description = "호감도 관련 API")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private static final int MAX_HISTORY_LIMIT = 50;

    @Operation(
            summary = "즐겨찾기 리스트 조회",
            description = "닉네임으로 필터링 가능하며, 페이지네이션과 정렬을 지원합니다."
    )
    @GetMapping("/list")
    public ModelAndView favoriteList(@PageableDefault Pageable pageable, @RequestParam(required = false) String nickName) {
        log.info("[GET][/favorite/list]");
        String safeNickName = Optional.ofNullable(nickName).map(HtmlUtils::htmlEscape).orElse("");
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("favorite").descending());
        Page<FavoriteEntity> favoriteList =
                StringUtils.isBlank(safeNickName) ? favoriteService.getList(page) : favoriteService.getByNickName(page, safeNickName);

        return new ModelAndView("index", "favoriteList", favoriteList);
    }

    @Operation(summary = "호감도 히스토리 조회", description = "ADMIN은 전체 조회, 그 외는 본인 계정만 조회 가능합니다.")
    @GetMapping("/history")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
    public List<FavoriteHistoryResponse> favoriteHistory(
            @RequestParam String userId,
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_HISTORY_LIMIT);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d", Locale.KOREA);
        return favoriteService.getHistory(userId, safeLimit).stream()
                .map(history -> FavoriteHistoryResponse.builder()
                        .favorite(history.getFavorite())
                        .history(history.getHistory())
                        .date(history.getModifyDate() != null
                                ? history.getModifyDate().format(formatter)
                                : "-")
                        .build())
                .toList();
    }
}
