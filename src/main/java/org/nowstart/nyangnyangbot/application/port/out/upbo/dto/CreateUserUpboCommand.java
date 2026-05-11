package org.nowstart.nyangnyangbot.application.port.out.upbo.dto;

import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;
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
