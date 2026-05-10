package org.nowstart.nyangnyangbot.domain.model;

import java.time.LocalDateTime;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;

public record UserUpbo(
        Long id,
        String userId,
        Long upboTemplateId,
        String nickNameSnapshot,
        String label,
        UpboStatus status,
        Integer exchangeFavoriteValue,
        RewardType rewardType,
        ConversionMode conversionMode,
        FavoriteSourceType sourceType,
        Long ledgerId,
        String publicDescription,
        String privateMemo,
        String actorId,
        LocalDateTime createdAt
) {
}
