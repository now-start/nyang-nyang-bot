package org.nowstart.nyangnyangbot.application.port.in.upbo;

import java.util.List;

public interface ManageUpboUseCase {

    List<UpboTemplateResult> getActiveTemplates();

    UpboTemplateResult createTemplate(UpboTemplateCreateCommand command);

    UserUpboResult applyUpbo(UpboApplyCommand command, String actorId);

    record UpboTemplateCreateCommand(
            String label,
            String description,
            Integer displayOrder,
            Integer exchangeFavoriteValue,
            String rewardType,
            String conversionMode
    ) {
    }

    record UpboApplyCommand(
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
    }

    record UpboTemplateResult(
            Long id,
            String label,
            String description,
            Boolean active,
            Integer displayOrder,
            Integer exchangeFavoriteValue,
            String rewardType,
            String conversionMode
    ) {
    }

    record UserUpboResult(
            Long id,
            String userId,
            String nickNameSnapshot,
            String label,
            String status,
            Integer exchangeFavoriteValue,
            String rewardType,
            String conversionMode,
            Long ledgerId,
            String publicDescription,
            java.time.LocalDateTime createdAt
    ) {
    }
}
