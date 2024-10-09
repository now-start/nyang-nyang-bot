package org.nowstart.chzzk_like_bot.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.chzzk_like_bot.data.entity.FavoriteEntity;
import org.nowstart.chzzk_like_bot.data.entity.FavoriteHistoryEntity;
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

    public void addFavorite(String userId, String nickName, int addFavorite, String history) {
        FavoriteEntity favoriteEntity = favoriteRepository.findByUserId(userId).orElse(
            FavoriteEntity.builder()
                .userId(userId)
                .nickName(nickName)
                .build());
        favoriteEntity.addFavorite(addFavorite);
        favoriteRepository.save(favoriteEntity);
        favoriteHistoryRepository.save(FavoriteHistoryEntity.builder()
            .favoriteEntity(favoriteEntity)
            .favorite(addFavorite)
            .history(history)
            .build());
    }

    public void deleteFavorite(String userId) {
        favoriteRepository.deleteById(userId);
    }

    public Page<FavoriteHistoryEntity> getFavoriteHistory(Pageable pageable, String userId) {
        return favoriteHistoryRepository.findByFavoriteEntityUserId(pageable, userId);
    }
}
