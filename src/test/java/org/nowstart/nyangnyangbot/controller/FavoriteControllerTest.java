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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.dto.WeeklyChatRankDto;
import org.nowstart.nyangnyangbot.data.dto.favorite.FavoriteListItemDto;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.repository.AuthorizationRepository;
import org.nowstart.nyangnyangbot.service.FavoriteService;
import org.nowstart.nyangnyangbot.service.WeeklyChatRankService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.ModelAndView;

@ExtendWith(MockitoExtension.class)
class FavoriteControllerTest {

    @Mock
    private FavoriteService favoriteService;

    @Mock
    private AuthorizationRepository authorizationRepository;

    @Mock
    private WeeklyChatRankService weeklyChatRankService;

    @InjectMocks
    private FavoriteController favoriteController;

    private List<FavoriteEntity> favoriteEntities;
    private List<WeeklyChatRankDto> weeklyChatRanks;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);

        favoriteEntities = Arrays.asList(
                favoriteEntity("user1", "유저1", 100),
                favoriteEntity("user2", "유저2", 50)
        );
        weeklyChatRanks = List.of(
                new WeeklyChatRankDto(1, "채터1", 11L),
                new WeeklyChatRankDto(2, "채터2", 7L)
        );
        given(weeklyChatRankService.getWeeklyRanks(5)).willReturn(weeklyChatRanks);
    }

    @Test
    void favoriteList_ShouldReturnAllFavorites_WhenNickNameIsNull() {
        // given
        Page<FavoriteEntity> expectedPage = new PageImpl<>(favoriteEntities, pageable, favoriteEntities.size());
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // when
        ModelAndView result = favoriteController.favoriteList(pageable, null, null);

        // then
        then(result.getViewName()).isEqualTo("index");
        then(result.getModel().get("landingMode")).isEqualTo(false);
        then(result.getModel().get("favoriteList")).isEqualTo(expectedPage);
        Page<FavoriteListItemDto> favoriteListView = (Page<FavoriteListItemDto>) result.getModel().get("favoriteListView");
        then(favoriteListView.getContent()).hasSize(2);
        then(favoriteListView.getContent().get(0).displayFavorite()).isEqualTo(100);
        then(favoriteListView.getContent().get(0).totalFavorite()).isEqualTo(100);
        then(result.getModel().get("weeklyChatRanks")).isEqualTo(weeklyChatRanks);
        BDDMockito.then(weeklyChatRankService).should().getWeeklyRanks(5);
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
        ModelAndView result = favoriteController.favoriteList(pageable, nickName, null);

        // then
        then(result.getViewName()).isEqualTo("index");
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
        ModelAndView result = favoriteController.favoriteList(pageable, "", null);

        // then
        then(result.getViewName()).isEqualTo("index");
        BDDMockito.then(favoriteService).should().getList(any(Pageable.class));
        BDDMockito.then(favoriteService).should(never()).getByNickName(any(), anyString());
    }

    @Test
    void favoriteList_ShouldReturnAllFavorites_WhenNickNameIsBlank() {
        // given
        Page<FavoriteEntity> expectedPage = new PageImpl<>(favoriteEntities, pageable, favoriteEntities.size());
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // when
        ModelAndView result = favoriteController.favoriteList(pageable, "   ", null);

        // then
        then(result.getViewName()).isEqualTo("index");
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
        favoriteController.favoriteList(pageable, maliciousNickName, null);

        // then
        BDDMockito.then(favoriteService).should().getByNickName(any(Pageable.class), eq(escapedNickName));
    }

    @Test
    void favoriteList_ShouldApplyDescendingSort_ByTotalFavorite() {
        // given
        Page<FavoriteEntity> expectedPage = new PageImpl<>(favoriteEntities, pageable, favoriteEntities.size());
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // when
        favoriteController.favoriteList(pageable, null, null);

        // then
        BDDMockito.then(favoriteService).should().getList(argThat(p ->
                p.getSort().equals(Sort.by("totalFavorite").descending())
        ));
    }

    @Test
    void favoriteList_ShouldPreservePageNumber() {
        // given
        Pageable page2 = PageRequest.of(2, 10);
        Page<FavoriteEntity> expectedPage = new PageImpl<>(favoriteEntities, page2, 100);
        given(favoriteService.getList(any(Pageable.class))).willReturn(expectedPage);

        // when
        favoriteController.favoriteList(page2, null, null);

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
        favoriteController.favoriteList(customPageable, null, null);

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
        ModelAndView result = favoriteController.favoriteList(pageable, specialNickName, null);

        // then
        then(result.getViewName()).isEqualTo("index");
        BDDMockito.then(favoriteService).should().getByNickName(any(Pageable.class), anyString());
    }

    @Test
    void favoriteList_ShouldReturnEmptyPage_WhenNoResults() {
        // given
        Page<FavoriteEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteService.getList(any(Pageable.class))).willReturn(emptyPage);

        // when
        ModelAndView result = favoriteController.favoriteList(pageable, null, null);

        // then
        then(result.getViewName()).isEqualTo("index");
        Page<FavoriteEntity> resultPage = (Page<FavoriteEntity>) result.getModel().get("favoriteList");
        then(resultPage.getContent()).isEmpty();
    }

    private FavoriteEntity favoriteEntity(String userId, String nickName, int score) {
        FavoriteEntity favoriteEntity = FavoriteEntity.builder()
                .userId(userId)
                .nickName(nickName)
                .build();
        setScore(favoriteEntity, score);
        return favoriteEntity;
    }

    private void setScore(FavoriteEntity favoriteEntity, int score) {
        if (ReflectionUtils.findField(FavoriteEntity.class, "totalFavorite") != null) {
            ReflectionTestUtils.setField(favoriteEntity, "totalFavorite", score);
            return;
        }
        ReflectionTestUtils.setField(favoriteEntity, "favorite", score);
    }
}
