package org.nowstart.nyangnyangbot.application.gateway.out.favorite;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.model.FavoriteHistoryView;
import org.nowstart.nyangnyangbot.domain.model.FavoriteSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FavoriteQueryPort {

    Page<FavoriteSummary> findAll(Pageable pageable);

    Page<FavoriteSummary> findByNickNameContains(Pageable pageable, String nickName);

    Optional<FavoriteSummary> findById(String userId);

    FavoriteSummary getOrCreate(String userId, String nickName);

    void updateNickName(String userId, String nickName);

    List<FavoriteHistoryView> findHistory(String userId, int limit);

    long countHistory(String userId);

    long countHistoryAfter(String userId, LocalDateTime createDate);
}
