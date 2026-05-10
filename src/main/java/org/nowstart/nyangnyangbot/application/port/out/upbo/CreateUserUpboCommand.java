package org.nowstart.nyangnyangbot.application.port.out.upbo;

import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.RewardType;
import org.nowstart.nyangnyangbot.data.type.UpboStatus;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;

public record CreateUserUpboCommand(
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
        String actorId
) {
}
