package org.nowstart.nyangnyangbot.adapter.in.web.upbo.request;

import jakarta.validation.constraints.NotBlank;
import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase.UpboTemplateCreateCommand;

public record UpboTemplateCreateRequest(
        @NotBlank(message = "label is required")
        String label,
        String description,
        Integer displayOrder,
        Integer exchangeFavoriteValue,
        @NotBlank(message = "rewardType is required")
        String rewardType,
        @NotBlank(message = "conversionMode is required")
        String conversionMode
) {

    public UpboTemplateCreateCommand toCreateTemplateCommand() {
        return new UpboTemplateCreateCommand(
                label, description, displayOrder, exchangeFavoriteValue, rewardType, conversionMode
        );
    }
}
