package org.nowstart.nyangnyangbot.adapter.in.web.command.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CommandUpdateRequest(
        String type,
        String trigger,
        String actionKey,
        String messageTemplate,
        Integer timerIntervalMinutes,
        Integer timerMinChatCount,
        Boolean active,
        String requiredRole,
        @Min(0)
        @Max(3600)
        Integer userCooldownSeconds
) {
}
