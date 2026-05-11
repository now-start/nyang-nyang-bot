package org.nowstart.nyangnyangbot.application.port.in.upbo.dto;

import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

public record UpboTemplateCreateCommand(
        String label,
        String description,
        Integer displayOrder,
        Integer exchangeFavoriteValue,
        RewardType rewardType,
        ConversionMode conversionMode
) {
}
