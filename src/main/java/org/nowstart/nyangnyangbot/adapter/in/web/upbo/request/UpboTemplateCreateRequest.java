package org.nowstart.nyangnyangbot.adapter.in.web.upbo.request;

import org.nowstart.nyangnyangbot.application.port.in.upbo.dto.UpboTemplateCreateCommand;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

public record UpboTemplateCreateRequest(
        String label,
        String description,
        Integer displayOrder,
        Integer exchangeFavoriteValue,
        RewardType rewardType,
        ConversionMode conversionMode
) {

    public UpboTemplateCreateCommand toCommand() {
        return new UpboTemplateCreateCommand(
                label, description, displayOrder, exchangeFavoriteValue, rewardType, conversionMode
        );
    }
}
