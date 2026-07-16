package org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository;

import java.time.LocalDateTime;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteHistoryRepository extends JpaRepository<FavoriteHistory, Long> {

    Page<FavoriteHistory> findByFavoriteAccountUserId(String userId, Pageable pageable);

    boolean existsByIdempotencyKey(String idempotencyKey);

    long countByFavoriteAccountUserId(String userId);

    long countByFavoriteAccountUserIdAndCreateDateAfter(String userId, LocalDateTime createDate);
}
