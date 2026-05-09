package org.nowstart.nyangnyangbot.service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.favorite.FavoriteHistoryDto;
import org.nowstart.nyangnyangbot.data.dto.favorite.FavoriteMeDto;
import org.nowstart.nyangnyangbot.data.entity.AuthorizationEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;
import org.nowstart.nyangnyangbot.repository.AuthorizationRepository;
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
    private final AuthorizationRepository authorizationRepository;

    public Page<FavoriteEntity> getList(Pageable pageable) {
        return favoriteRepository.findAll(pageable);
    }

    public Page<FavoriteEntity> getByNickName(Pageable pageable, String nickName) {
        return favoriteRepository.findByNickNameContains(pageable, nickName);
    }

    public List<FavoriteHistoryEntity> getHistory(String userId, int limit) {
        Pageable page = PageRequest.of(0, limit, Sort.by("createDate").descending());
        return favoriteHistoryRepository.findByFavoriteEntityUserId(userId, page).getContent();
    }

    public FavoriteMeDto getMyFavorite(String userId) {
        AuthorizationEntity authorizationEntity = authorizationRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Authorization user not found"));
        FavoriteEntity favoriteEntity = favoriteRepository.findById(userId)
                .orElse(FavoriteEntity.builder()
                        .userId(userId)
                        .nickName(authorizationEntity.getChannelName())
                        .favorite(0)
                        .build());

        LocalDateTime lastSeenAt = authorizationEntity.getFavoriteHistoryLastSeenAt();
        long unseenCount = lastSeenAt == null
                ? favoriteHistoryRepository.countByFavoriteEntityUserId(userId)
                : favoriteHistoryRepository.countByFavoriteEntityUserIdAndCreateDateAfter(userId, lastSeenAt);
        List<FavoriteHistoryDto> histories = getHistory(userId, 50).stream()
                .map(FavoriteHistoryDto::from)
                .toList();

        authorizationEntity.setFavoriteHistoryLastSeenAt(LocalDateTime.now());
        return new FavoriteMeDto(
                userId,
                favoriteEntity.getNickName(),
                favoriteEntity.getFavorite(),
                unseenCount,
                histories
        );
    }
}
