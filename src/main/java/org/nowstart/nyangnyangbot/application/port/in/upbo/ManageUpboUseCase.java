package org.nowstart.nyangnyangbot.application.port.in.upbo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public interface ManageUpboUseCase {

    List<UpboTemplateResult> getActiveTemplates();

    UpboTemplateResult createTemplate(
            @Valid @NotNull(message = "command is required") UpboTemplateCreateCommand command
    );

    UserUpboResult applyUpbo(
            @Valid @NotNull(message = "command is required") UpboApplyCommand command,
            String actorId
    );

    record UpboTemplateCreateCommand(
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
    }

    record UpboApplyCommand(
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
