package org.nowstart.nyangnyangbot.application.favorite;

import lombok.Builder;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;

@Builder
public record AdjustFavoriteCommand(
        String userId,
        String nickName,
        int delta,
        FavoriteSourceType sourceType,
        String sourceId,
        String displayCategory,
        String publicDescription,
        String privateMemo,
        Long correctionOfLedgerId,
        String actorId,
        String idempotencyKey,
        boolean allowNegativeBalance,
        boolean createIfMissing
) {
}
