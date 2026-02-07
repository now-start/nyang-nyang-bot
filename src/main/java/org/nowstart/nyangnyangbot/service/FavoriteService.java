package org.nowstart.nyangnyangbot.service;

import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.entity.ChannelEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;
import org.nowstart.nyangnyangbot.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final FavoriteHistoryRepository favoriteHistoryRepository;
    private final ChannelService channelService;

    public Page<FavoriteEntity> getList(Pageable pageable) {
        ChannelEntity ownerChannel = channelService.getDefaultChannel();
        return favoriteRepository.findByOwnerChannelId(pageable, ownerChannel.getId());
    }

    public Page<FavoriteEntity> getByNickName(Pageable pageable, String nickName) {
        ChannelEntity ownerChannel = channelService.getDefaultChannel();
        return favoriteRepository.findByOwnerChannelIdAndTargetChannelNameContains(pageable, ownerChannel.getId(), nickName);
    }

    public List<FavoriteHistoryEntity> getHistory(String userId, int limit) {
        Pageable page = PageRequest.of(0, limit, Sort.by("modifyDate").descending());
        ChannelEntity ownerChannel = channelService.getDefaultChannel();
        return favoriteHistoryRepository
                .findByFavoriteOwnerChannelIdAndFavoriteTargetChannelId(ownerChannel.getId(), userId, page)
                .getContent();
    }
}
