package org.nowstart.nyangnyangbot.application.port.in.favorite;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QueryFavoriteUseCase {

    Page<FavoriteSummaryResult> getList(Pageable pageable);

    Page<FavoriteSummaryResult> getByNickName(Pageable pageable, String nickName);

    List<FavoriteHistoryResult> getHistory(String userId, int limit);

    FavoriteMeResult getMyFavorite(String userId);

    Optional<String> getCurrentNickName(String userId);

    record FavoriteSummaryResult(
            String userId,
            String nickName,
            Integer favorite
    ) {
    }

    record FavoriteHistoryResult(
            Long ledgerId,
            String channelId,
            String nickNameSnapshot,
            Integer delta,
            Integer balanceAfter,
            String sourceType,
            String displayCategory,
            String publicDescription,
            boolean correction,
            Integer favorite,
            String history,
            LocalDateTime createdAt
    ) {
    }

    record FavoriteMeResult(
            String userId,
            String nickName,
            Integer favorite,
            Integer rank,
            List<FavoriteHistoryResult> histories
    ) {
    }
}
