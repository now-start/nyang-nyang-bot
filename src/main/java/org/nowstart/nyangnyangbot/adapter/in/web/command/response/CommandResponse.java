package org.nowstart.nyangnyangbot.adapter.in.web.command.response;

import java.time.LocalDateTime;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.CommandResult;

public record CommandResponse(
        Long id,
        String type,
        String trigger,
        String actionKey,
        String messageTemplate,
        Integer timerIntervalMinutes,
        Integer timerMinChatCount,
        boolean active,
        String requiredRole,
        Integer userCooldownSeconds,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static CommandResponse from(CommandResult result) {
        return new CommandResponse(
                result.id(),
                result.type(),
                result.trigger(),
                result.actionKey(),
                result.messageTemplate(),
                result.timerIntervalMinutes(),
                result.timerMinChatCount(),
                result.active(),
                result.requiredRole(),
                result.userCooldownSeconds(),
                result.createdBy(),
                result.updatedBy(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
