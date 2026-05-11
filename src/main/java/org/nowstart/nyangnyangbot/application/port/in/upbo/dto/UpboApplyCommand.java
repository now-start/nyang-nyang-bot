package org.nowstart.nyangnyangbot.application.port.in.upbo.dto;

import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

public record UpboApplyCommand(
        String userId,
        String nickName,
        Long templateId,
        String label,
        RewardType rewardType,
        ConversionMode conversionMode,
        Integer exchangeFavoriteValue,
        String publicDescription,
        String privateMemo
) {
}
