package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.entity.ChannelEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private FavoriteHistoryRepository favoriteHistoryRepository;

    @Mock
    private ChannelService channelService;

    @InjectMocks
    private FavoriteService favoriteService;

    private List<FavoriteEntity> favoriteEntities;
    private Pageable pageable;
    private ChannelEntity ownerChannel;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
        ownerChannel = ChannelEntity.builder()
                .id("owner")
                .name("Owner")
                .build();

        favoriteEntities = Arrays.asList(
                FavoriteEntity.builder()
                        .ownerChannel(ownerChannel)
                        .targetChannel(channel("user1", "?ŒìŠ¤?¸ìœ ?€1"))
                        .favorite(100)
                        .build(),
                FavoriteEntity.builder()
                        .ownerChannel(ownerChannel)
                        .targetChannel(channel("user2", "?ŒìŠ¤?¸ìœ ?€2"))
                        .favorite(50)
                        .build(),
                FavoriteEntity.builder()
                        .ownerChannel(ownerChannel)
                        .targetChannel(channel("user3", "? ì?3"))
                        .favorite(30)
                        .build()
        );
        given(channelService.getDefaultChannel()).willReturn(ownerChannel);
    }

    @Test
    void getList_ShouldReturnAllFavorites() {
        // given
        Page<FavoriteEntity> expectedPage = new PageImpl<>(favoriteEntities, pageable, favoriteEntities.size());
        given(favoriteRepository.findByOwnerChannelId(pageable, ownerChannel.getId())).willReturn(expectedPage);

        // when
        Page<FavoriteEntity> result = favoriteService.getList(pageable);

        // then
        then(result).isNotNull();
        then(result.getContent()).hasSize(3);
        then(result.getTotalElements()).isEqualTo(3);
        BDDMockito.then(favoriteRepository).should().findByOwnerChannelId(pageable, ownerChannel.getId());
    }

    @Test
    void getList_ShouldReturnEmptyPage_WhenNoFavorites() {
        // given
        Page<FavoriteEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteRepository.findByOwnerChannelId(pageable, ownerChannel.getId())).willReturn(emptyPage);

        // when
        Page<FavoriteEntity> result = favoriteService.getList(pageable);

        // then
        then(result).isNotNull();
        then(result.getContent()).isEmpty();
        then(result.getTotalElements()).isZero();
        BDDMockito.then(favoriteRepository).should().findByOwnerChannelId(pageable, ownerChannel.getId());
    }

    @Test
    void getByNickName_ShouldReturnFilteredFavorites() {
        // given
        String nickName = "?ŒìŠ¤??;
        List<FavoriteEntity> filteredList = Arrays.asList(
                favoriteEntities.get(0),
                favoriteEntities.get(1)
        );
        Page<FavoriteEntity> expectedPage = new PageImpl<>(filteredList, pageable, filteredList.size());
        given(favoriteRepository.findByOwnerChannelIdAndTargetChannelNameContains(pageable, ownerChannel.getId(), nickName))
                .willReturn(expectedPage);

        // when
        Page<FavoriteEntity> result = favoriteService.getByNickName(pageable, nickName);

        // then
        then(result).isNotNull();
        then(result.getContent()).hasSize(2);
        then(result.getContent()).allMatch(entity -> entity.getNickName().contains(nickName));
        BDDMockito.then(favoriteRepository)
                .should()
                .findByOwnerChannelIdAndTargetChannelNameContains(pageable, ownerChannel.getId(), nickName);
    }

    @Test
    void getByNickName_ShouldReturnEmptyPage_WhenNoMatch() {
        // given
        String nickName = "ì¡´ìž¬?˜ì??ŠëŠ”? ì?";
        Page<FavoriteEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteRepository.findByOwnerChannelIdAndTargetChannelNameContains(pageable, ownerChannel.getId(), nickName))
                .willReturn(emptyPage);

        // when
        Page<FavoriteEntity> result = favoriteService.getByNickName(pageable, nickName);

        // then
        then(result).isNotNull();
        then(result.getContent()).isEmpty();
        BDDMockito.then(favoriteRepository)
                .should()
                .findByOwnerChannelIdAndTargetChannelNameContains(pageable, ownerChannel.getId(), nickName);
    }

    @Test
    void getByNickName_ShouldHandleSpecialCharacters() {
        // given
        String nickName = "? ì?@#$";
        Page<FavoriteEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(favoriteRepository.findByOwnerChannelIdAndTargetChannelNameContains(pageable, ownerChannel.getId(), nickName))
                .willReturn(emptyPage);

        // when
        Page<FavoriteEntity> result = favoriteService.getByNickName(pageable, nickName);

        // then
        then(result).isNotNull();
        BDDMockito.then(favoriteRepository)
                .should()
                .findByOwnerChannelIdAndTargetChannelNameContains(pageable, ownerChannel.getId(), nickName);
    }

    @Test
    void getList_ShouldHandleDifferentPageSizes() {
        // given
        Pageable largePageable = PageRequest.of(0, 100);
        Page<FavoriteEntity> expectedPage = new PageImpl<>(favoriteEntities, largePageable, favoriteEntities.size());
        given(favoriteRepository.findByOwnerChannelId(largePageable, ownerChannel.getId())).willReturn(expectedPage);

        // when
        Page<FavoriteEntity> result = favoriteService.getList(largePageable);

        // then
        then(result).isNotNull();
        then(result.getContent()).hasSize(3);
        BDDMockito.then(favoriteRepository).should().findByOwnerChannelId(largePageable, ownerChannel.getId());
    }

    private ChannelEntity channel(String id, String name) {
        return ChannelEntity.builder()
                .id(id)
                .name(name)
                .build();
    }
}


