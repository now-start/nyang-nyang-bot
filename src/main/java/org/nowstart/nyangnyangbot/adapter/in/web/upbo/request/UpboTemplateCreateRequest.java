package org.nowstart.nyangnyangbot.adapter.in.web.upbo.request;

import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase.UpboTemplateCreateCommand;

public record UpboTemplateCreateRequest(
        String label,
        String description,
        Integer displayOrder,
        Integer exchangeFavoriteValue,
        String rewardType,
        String conversionMode
) {

    public UpboTemplateCreateCommand toCreateTemplateCommand() {
        return new UpboTemplateCreateCommand(
                label, description, displayOrder, exchangeFavoriteValue, rewardType, conversionMode
        );
    }
}
