package org.nowstart.nyangnyangbot.application.port.out.favorite;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FavoriteQueryPort {

    Page<SummaryResult> findAll(Pageable pageable);

    Page<SummaryResult> findByNickNameContains(Pageable pageable, String nickName);

    Optional<SummaryResult> findById(String userId);

    SummaryResult getOrCreate(String userId, String nickName);

    void updateNickName(String userId, String nickName);

    List<HistoryResult> findHistory(String userId, int limit);

    long countHistory(String userId);

    long countHistoryAfter(String userId, LocalDateTime createDate);

    long countByFavoriteGreaterThan(Integer favorite);

    record SummaryResult(
            String userId,
            String nickName,
            Integer favorite
    ) {
    }

    record HistoryResult(
            Long ledgerId,
            String channelId,
            String nickNameSnapshot,
            Integer delta,
            Integer balanceAfter,
            FavoriteSourceType sourceType,
            String displayCategory,
            String publicDescription,
            boolean correction,
            Integer favorite,
            String history,
            LocalDateTime createdAt
    ) {
    }
}
