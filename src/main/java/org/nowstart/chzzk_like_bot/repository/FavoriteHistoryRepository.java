// FavoriteHistoryRepository.java
package org.nowstart.chzzk_like_bot.repository;

import java.util.List;
import org.nowstart.chzzk_like_bot.entity.FavoriteHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteHistoryRepository extends JpaRepository<FavoriteHistoryEntity, Long> {
    List<FavoriteHistoryEntity> findByFavoriteEntityUserId(String userId);
}