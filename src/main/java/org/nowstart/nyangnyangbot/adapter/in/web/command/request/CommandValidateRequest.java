package org.nowstart.nyangnyangbot.adapter.in.web.command.request;

public record CommandValidateRequest(
        Long commandId,
        String type,
        String trigger,
        String actionKey,
        String messageTemplate,
        Integer timerIntervalMinutes,
        Integer timerMinChatCount,
        String requiredRole,
        Integer userCooldownSeconds
) {
}
