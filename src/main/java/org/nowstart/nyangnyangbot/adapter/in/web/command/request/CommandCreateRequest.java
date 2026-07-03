package org.nowstart.nyangnyangbot.adapter.in.web.command.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record CommandCreateRequest(
        @NotBlank(message = "type is required")
        String type,
        String trigger,
        String actionKey,
        String messageTemplate,
        Integer timerIntervalMinutes,
        Integer timerMinChatCount,
        Boolean active,
        String requiredRole,
        @PositiveOrZero(message = "userCooldownSeconds must be between 0 and 3600")
        @Max(value = 3600, message = "userCooldownSeconds must be between 0 and 3600")
        Integer userCooldownSeconds
) {
}
