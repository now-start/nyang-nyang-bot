package org.nowstart.nyangnyangbot.controller;

import io.micrometer.common.util.StringUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;
import org.nowstart.nyangnyangbot.service.FavoriteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/favorite")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping("/list")
    public ModelAndView favoriteList(@PageableDefault(size = 10) Pageable pageable, @RequestParam(required = false) String nickName) {
        log.info("[GET][/favorite/list]");
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("favorite").descending());
        Page<FavoriteEntity> favoriteList = StringUtils.isBlank(nickName) ? favoriteService.getList(page) : favoriteService.getByNickName(page, nickName);
        favoriteList.getContent().forEach(favoriteEntity -> {
            List<FavoriteHistoryEntity> favoriteHistoryEntityList = favoriteEntity.getFavoriteHistoryEntityList();
            favoriteHistoryEntityList.sort((a, b) -> b.getCreateDate().compareTo(a.getCreateDate()));
        });
        return new ModelAndView("FavoriteList", "favoriteList", favoriteList);
    }
}
