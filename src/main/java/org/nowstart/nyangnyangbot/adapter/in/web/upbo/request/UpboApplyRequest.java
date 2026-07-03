package org.nowstart.nyangnyangbot.adapter.in.web.upbo.request;

import jakarta.validation.constraints.NotBlank;
import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase.UpboApplyCommand;

public record UpboApplyRequest(
        @NotBlank(message = "userId is required")
        String userId,
        String nickName,
        Long templateId,
        String label,
        String rewardType,
        String conversionMode,
        Integer exchangeFavoriteValue,
        @NotBlank(message = "publicDescription is required")
        String publicDescription,
        @NotBlank(message = "privateMemo is required")
        String privateMemo
) {

    public UpboApplyCommand toApplyUpboCommand() {
        return new UpboApplyCommand(
                userId, nickName, templateId, label, rewardType, conversionMode,
                exchangeFavoriteValue, publicDescription, privateMemo
        );
    }
}
