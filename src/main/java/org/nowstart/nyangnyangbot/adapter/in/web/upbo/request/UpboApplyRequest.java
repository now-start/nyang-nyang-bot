package org.nowstart.nyangnyangbot.adapter.in.web.upbo.request;

import org.nowstart.nyangnyangbot.application.port.in.upbo.dto.UpboApplyCommand;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

public record UpboApplyRequest(
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

    public UpboApplyCommand toCommand() {
        return new UpboApplyCommand(
                userId, nickName, templateId, label, rewardType, conversionMode,
                exchangeFavoriteValue, publicDescription, privateMemo
        );
    }
}
