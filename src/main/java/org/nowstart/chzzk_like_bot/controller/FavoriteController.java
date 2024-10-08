package org.nowstart.chzzk_like_bot.controller;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.nowstart.chzzk_like_bot.entity.FavoriteEntity;
import org.nowstart.chzzk_like_bot.service.FavoriteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping("/favorite/list")
    public ModelAndView favoriteList(@PageableDefault(size = 10) Pageable pageable, @RequestParam(required = false) String searchId) {
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("favorite").descending());
        Page<FavoriteEntity> favoriteList = StringUtils.isBlank(searchId) ? favoriteService.getList(page) : favoriteService.getByNickName(searchId, page);
        return new ModelAndView("FavoriteList", "favoriteList", favoriteList);
    }

    @PostMapping("/favorite/add")
    public ModelAndView addUser(@RequestParam String nickName, @RequestParam int favorite) {
        favoriteService.addFavorite(nickName, favorite);
        return new ModelAndView("redirect:/favorite/list");
    }

    @PostMapping("/favorite/delete")
    public ModelAndView addUser(@RequestParam String userId) {
        favoriteService.deleteFavorite(userId);
        return new ModelAndView("redirect:/favorite/list");
    }
}
