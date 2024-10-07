package org.nowstart.chzzk_like_bot.repository;

import org.nowstart.chzzk_like_bot.entity.FavoriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends JpaRepository<FavoriteEntity, String> {
    FavoriteEntity findByUserId(String userId);

}
