package org.nowstart.nyangnyangbot.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;
import org.nowstart.nyangnyangbot.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
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

    public Page<FavoriteEntity> getByNickName(Pageable pageable, String nickName) {
        return favoriteRepository.findByNickNameContains(pageable, nickName);
    }

    public void addFavorite(String userId, String nickName, int addFavorite, String history) {
        FavoriteEntity favoriteEntity = favoriteRepository.findByUserId(userId).orElse(
            FavoriteEntity.builder()
                .userId(userId)
                .nickName(nickName)
                .build());

        favoriteHistoryRepository.save(FavoriteHistoryEntity.builder()
            .favoriteEntity(favoriteEntity.addFavorite(addFavorite))
            .favorite(favoriteEntity.getFavorite())
            .history(history)
            .build());
    }

    public void deleteFavorite(String userId) {
        favoriteRepository.deleteById(userId);
    }
}
