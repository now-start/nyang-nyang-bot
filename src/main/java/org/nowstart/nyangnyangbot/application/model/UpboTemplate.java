package org.nowstart.nyangnyangbot.application.model;

import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.RewardType;

public record UpboTemplate(
        Long id,
        String label,
        String description,
        boolean active,
        Integer displayOrder,
        Integer exchangeFavoriteValue,
        RewardType rewardType,
        ConversionMode conversionMode
) {
}
