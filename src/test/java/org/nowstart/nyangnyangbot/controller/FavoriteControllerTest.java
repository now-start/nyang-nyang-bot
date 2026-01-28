package org.nowstart.nyangnyangbot.controller;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.service.FavoriteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.servlet.ModelAndView;

@Disabled
@ExtendWith(MockitoExtension.class)
class FavoriteControllerTest {

    @Mock
    private FavoriteService favoriteService;

    @InjectMocks
    private FavoriteController favoriteController;

    private List<FavoriteEntity> favoriteEntities;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);

        favoriteEntities = Arrays.asList(
                FavoriteEntity.builder()
                        .userId("user1")
                        .nickName("유저1")
                        .favorite(100)
                        .build(),
                FavoriteEntity.builder()
                        .userId("user2")
                        .nickName("유저2")
                        .favorite(50)
                        .build()
        );
    }

    @Test
    void favoriteList_ShouldReturnAllFavorites_WhenNickNameIsNull() {
        // given
        Page<FavoriteEntity> expectedPage = new PageImpl<>(favoriteEntities, pageable, favoriteEntities.size());
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // when
        ModelAndView result = favoriteController.favoriteList(pageable, null);

        // then
        then(result.getViewName()).isEqualTo("FavoriteList");
        then(result.getModel().get("favoriteList")).isEqualTo(expectedPage);
        BDDMockito.then(favoriteService).should().getList(any(Pageable.class));
        BDDMockito.then(favoriteService).should(never()).getByNickName(any(), anyString());
    }

    @Test
    void favoriteList_ShouldReturnFilteredFavorites_WhenNickNameProvided() {
        // given
        String nickName = "유저1";
        List<FavoriteEntity> filteredList = Collections.singletonList(favoriteEntities.get(0));
        Page<FavoriteEntity> expectedPage = new PageImpl<>(filteredList, pageable, filteredList.size());

        given(favoriteService.getByNickName(any(Pageable.class), eq(nickName))).willReturn(expectedPage);

        // when
        ModelAndView result = favoriteController.favoriteList(pageable, nickName);

        // then
        then(result.getViewName()).isEqualTo("FavoriteList");
        then(result.getModel().get("favoriteList")).isEqualTo(expectedPage);
        BDDMockito.then(favoriteService).should().getByNickName(any(Pageable.class), eq(nickName));
        BDDMockito.then(favoriteService).should(never()).getList(any());
    }

    @Test
    void favoriteList_ShouldReturnAllFavorites_WhenNickNameIsEmpty() {
        // given
        Page<FavoriteEntity> expectedPage = new PageImpl<>(favoriteEntities, pageable, favoriteEntities.size());
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // when
        ModelAndView result = favoriteController.favoriteList(pageable, "");

        // then
        then(result.getViewName()).isEqualTo("FavoriteList");
        BDDMockito.then(favoriteService).should().getList(any(Pageable.class));
        BDDMockito.then(favoriteService).should(never()).getByNickName(any(), anyString());
    }

    @Test
    void favoriteList_ShouldReturnAllFavorites_WhenNickNameIsBlank() {
        // given
        Page<FavoriteEntity> expectedPage = new PageImpl<>(favoriteEntities, pageable, favoriteEntities.size());
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // when
        ModelAndView result = favoriteController.favoriteList(pageable, "   ");

        // then
        then(result.getViewName()).isEqualTo("FavoriteList");
        BDDMockito.then(favoriteService).should().getList(any(Pageable.class));
    }

    @Test
    void favoriteList_ShouldEscapeHtml_InNickName() {
        // given
        String maliciousNickName = "<script>alert('xss')</script>";
        String escapedNickName = "&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;";

        Page<FavoriteEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteService.getByNickName(any(Pageable.class), eq(escapedNickName))).willReturn(emptyPage);

        // when
        favoriteController.favoriteList(pageable, maliciousNickName);

        // then
        BDDMockito.then(favoriteService).should().getByNickName(any(Pageable.class), eq(escapedNickName));
    }

    @Test
    void favoriteList_ShouldApplyDescendingSort_ByFavorite() {
        // given
        Page<FavoriteEntity> expectedPage = new PageImpl<>(favoriteEntities, pageable, favoriteEntities.size());
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // when
        favoriteController.favoriteList(pageable, null);

        // then
        BDDMockito.then(favoriteService).should().getList(argThat(p ->
                p.getSort().equals(Sort.by("favorite").descending())
        ));
    }

    @Test
    void favoriteList_ShouldPreservePageNumber() {
        // given
        Pageable page2 = PageRequest.of(2, 10);
        Page<FavoriteEntity> expectedPage = new PageImpl<>(favoriteEntities, page2, 100);
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // when
        favoriteController.favoriteList(page2, null);

        // then
        BDDMockito.then(favoriteService).should().getList(argThat(p -> p.getPageNumber() == 2));
    }

    @Test
    void favoriteList_ShouldPreservePageSize() {
        // given
        Pageable customPageable = PageRequest.of(0, 50);
        Page<FavoriteEntity> expectedPage = new PageImpl<>(favoriteEntities, customPageable, favoriteEntities.size());
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // when
        favoriteController.favoriteList(customPageable, null);

        // then
        BDDMockito.then(favoriteService).should().getList(argThat(p -> p.getPageSize() == 50));
    }

    @Test
    void favoriteList_ShouldHandleSpecialCharactersInNickName() {
        // given
        String specialNickName = "유저@#$%^&*()";
        Page<FavoriteEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteService.getByNickName(any(Pageable.class), anyString())).willReturn(emptyPage);

        // when
        ModelAndView result = favoriteController.favoriteList(pageable, specialNickName);

        // then
        then(result.getViewName()).isEqualTo("FavoriteList");
        BDDMockito.then(favoriteService).should().getByNickName(any(Pageable.class), anyString());
    }

    @Test
    void favoriteList_ShouldReturnEmptyPage_WhenNoResults() {
        // given
        Page<FavoriteEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteService.getList(any(Pageable.class))).willReturn(emptyPage);

        // when
        ModelAndView result = favoriteController.favoriteList(pageable, null);

        // then
        then(result.getViewName()).isEqualTo("FavoriteList");
        Page<FavoriteEntity> resultPage = (Page<FavoriteEntity>) result.getModel().get("favoriteList");
        then(resultPage.getContent()).isEmpty();
    }
}
