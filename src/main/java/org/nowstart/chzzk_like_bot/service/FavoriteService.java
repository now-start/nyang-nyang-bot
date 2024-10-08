package org.nowstart.chzzk_like_bot.service;

import jakarta.transaction.Transactional;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.chzzk_like_bot.entity.FavoriteEntity;
import org.nowstart.chzzk_like_bot.entity.FavoriteHistoryEntity;
import org.nowstart.chzzk_like_bot.repository.FavoriteHistoryRepository;
import org.nowstart.chzzk_like_bot.repository.FavoriteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final FavoriteHistoryRepository favoriteHistoryRepository;

    public Page<FavoriteEntity> getList(Pageable pageable) {
        return favoriteRepository.findAll(pageable);
    }

    public Page<FavoriteEntity> getByNickName(String nickName, Pageable pageable) {
        return favoriteRepository.findByNickNameContains(pageable, nickName);
    }

    public Optional<FavoriteEntity> getByUserId(String nickName) {
        return favoriteRepository.findByUserId(nickName);
    }

    public void addFavorite(String userId, String nickName, int favorite, String history) {
        FavoriteEntity favoriteEntity = getByUserId(userId).orElse(
            FavoriteEntity.builder()
                .userId(userId)
                .nickName(nickName)
                .build());
        favoriteEntity.addFavorite(favorite);
        FavoriteHistoryEntity favoriteHistoryEntity = FavoriteHistoryEntity.builder()
            .favoriteEntity(favoriteEntity)
            .favorite(favorite)
            .history(history)
            .build();
        favoriteHistoryRepository.save(favoriteHistoryEntity);
    }

    public void deleteFavorite(String userId) {
        favoriteRepository.deleteById(userId);
    }

    public Page<FavoriteHistoryEntity> getFavoriteHistory(Pageable pageable, String userId) {
        return favoriteHistoryRepository.findByFavoriteEntityUserId(pageable, userId);
    }
}
