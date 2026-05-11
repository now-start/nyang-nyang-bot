package org.nowstart.nyangnyangbot.adapter.in.web.upbo.response;

import org.nowstart.nyangnyangbot.domain.model.UpboTemplate;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

public record UpboTemplateResponse(
        Long id,
        String label,
        String description,
        Boolean active,
        Integer displayOrder,
        Integer exchangeFavoriteValue,
        RewardType rewardType,
        ConversionMode conversionMode
) {

    public static UpboTemplateResponse from(UpboTemplate entity) {
        return new UpboTemplateResponse(
                entity.id(),
                entity.label(),
                entity.description(),
                entity.active(),
                entity.displayOrder(),
                entity.exchangeFavoriteValue(),
                entity.rewardType(),
                entity.conversionMode()
        );
    }
}
