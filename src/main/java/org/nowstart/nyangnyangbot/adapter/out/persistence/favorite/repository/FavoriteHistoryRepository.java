package org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository;

// FavoriteHistoryRepository.java


import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteHistoryEntity;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteHistoryRepository extends JpaRepository<FavoriteHistoryEntity, Long> {

    Page<FavoriteHistoryEntity> findByFavoriteEntityUserId(String userId, Pageable pageable);

    boolean existsByIdempotencyKey(String idempotencyKey);

    long countByFavoriteEntityUserId(String userId);

    long countByFavoriteEntityUserIdAndCreateDateAfter(String userId, LocalDateTime createDate);
}
