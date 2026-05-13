package org.nowstart.nyangnyangbot.adapter.in.web.upbo.request;

import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase.UpboApplyCommand;

public record UpboApplyRequest(
        String userId,
        String nickName,
        Long templateId,
        String label,
        String rewardType,
        String conversionMode,
        Integer exchangeFavoriteValue,
        String publicDescription,
        String privateMemo
) {

    public UpboApplyCommand toApplyUpboCommand() {
        return new UpboApplyCommand(
                userId, nickName, templateId, label, rewardType, conversionMode,
                exchangeFavoriteValue, publicDescription, privateMemo
        );
    }
}
