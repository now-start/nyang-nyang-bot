// FavoriteHistoryRepository.java
package org.nowstart.chzzk_like_bot.repository;

import org.nowstart.chzzk_like_bot.data.entity.FavoriteHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteHistoryRepository extends JpaRepository<FavoriteHistoryEntity, Long> {
    Page<FavoriteHistoryEntity> findByFavoriteEntityUserId(Pageable pageable, String userId);
}