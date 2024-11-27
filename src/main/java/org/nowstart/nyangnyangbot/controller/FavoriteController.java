package org.nowstart.nyangnyangbot.controller;

import io.micrometer.common.util.StringUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.config.GoogleConfig;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
@RequiredArgsConstructor
public class FavoriteController {

    private final GoogleConfig googleConfig;
    private final FavoriteService favoriteService;

    @GetMapping("/favorite/list")
    public ModelAndView favoriteList(@PageableDefault(size = 10) Pageable pageable, @RequestParam(required = false) String searchId) {
        log.info("[GET][/favorite/list]");
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("favorite").descending());
        Page<FavoriteEntity> favoriteList = StringUtils.isBlank(searchId) ? favoriteService.getList(page) : favoriteService.getByNickName(page, searchId);
        favoriteList.getContent().forEach(favoriteEntity -> {
            List<FavoriteHistoryEntity> favoriteHistoryEntityList = favoriteEntity.getFavoriteHistoryEntityList();
            favoriteHistoryEntityList.sort((a, b) -> b.getCreateDate().compareTo(a.getCreateDate()));
        });
        return new ModelAndView("FavoriteList", "favoriteList", favoriteList);
    }

    @PostMapping("/favorite/add")
    public ModelAndView addUser(@RequestParam String userId, @RequestParam String nickName, @RequestParam int favorite, @RequestParam String history) {
        log.info("[POST][/favorite/add]");
        favoriteService.addFavorite(userId, nickName, favorite, history);
        return new ModelAndView("redirect:/favorite/list");
    }

    @PostMapping("/favorite/delete")
    public ModelAndView addUser(@RequestParam String userId) {
        log.info("[POST][/favorite/delete]");
        favoriteService.deleteFavorite(userId);
        return new ModelAndView("redirect:/favorite/list");
    }

    @ResponseBody
    @PostMapping("/favorite/sync")
    public String sync() {
        log.info("[POST][/favorite/sync]");
        googleConfig.syncDatabase();
        return "success";
    }
}
