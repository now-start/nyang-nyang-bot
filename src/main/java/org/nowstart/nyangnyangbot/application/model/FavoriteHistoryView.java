package org.nowstart.nyangnyangbot.application.model;

import java.time.LocalDateTime;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;

public record FavoriteHistoryView(
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
