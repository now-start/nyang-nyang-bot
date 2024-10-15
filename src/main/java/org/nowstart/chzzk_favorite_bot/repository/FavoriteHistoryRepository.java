// FavoriteHistoryRepository.java
package org.nowstart.chzzk_favorite_bot.repository;

import org.nowstart.chzzk_favorite_bot.data.entity.FavoriteHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteHistoryRepository extends JpaRepository<FavoriteHistoryEntity, Long> {
}