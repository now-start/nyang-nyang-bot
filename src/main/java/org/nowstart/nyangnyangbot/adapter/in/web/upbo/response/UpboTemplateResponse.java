package org.nowstart.nyangnyangbot.adapter.in.web.upbo.response;

import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase.UpboTemplateResult;

public record UpboTemplateResponse(
        Long id,
        String label,
        String description,
        Boolean active,
        Integer displayOrder,
        Integer exchangeFavoriteValue,
        String rewardType,
        String conversionMode
) {

    public static UpboTemplateResponse from(UpboTemplateResult result) {
        return new UpboTemplateResponse(
                result.id(),
                result.label(),
                result.description(),
                result.active(),
                result.displayOrder(),
                result.exchangeFavoriteValue(),
                result.rewardType(),
                result.conversionMode()
        );
    }
}
