package org.nowstart.nyangnyangbot.application.port.in.command;

import java.time.LocalDateTime;
import java.util.List;

public interface ManageCommandUseCase {

    List<CommandResult> getCommands();

    CommandResult createCommand(CreateCommand request);

    CommandResult updateCommand(Long commandId, UpdateCommand request);

    PreviewResult preview(PreviewCommand request);

    ValidationResult validate(ValidateCommand request);

    record CreateCommand(
            String type,
            String trigger,
            String actionKey,
            String messageTemplate,
            Integer timerIntervalMinutes,
            Integer timerMinChatCount,
            Boolean active,
            String requiredRole,
            Integer userCooldownSeconds,
            String actorId
    ) {
    }

    record UpdateCommand(
            String type,
            String trigger,
            String actionKey,
            String messageTemplate,
            Integer timerIntervalMinutes,
            Integer timerMinChatCount,
            Boolean active,
            String requiredRole,
            Integer userCooldownSeconds,
            String actorId
    ) {
    }

    record PreviewCommand(
            String messageTemplate,
            String nickname,
            String command,
            String args,
            String arg1,
            String arg2,
            Integer favorite
    ) {
    }

    record ValidateCommand(
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

    record CommandResult(
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
    }

    record PreviewResult(String message) {
    }

    record ValidationResult(boolean valid, List<String> errors) {
    }
}
