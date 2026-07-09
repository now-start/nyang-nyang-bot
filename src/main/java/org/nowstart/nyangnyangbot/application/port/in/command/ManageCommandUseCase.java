package org.nowstart.nyangnyangbot.application.port.in.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public interface ManageCommandUseCase {

    List<CommandResult> getCommands();

    CommandResult createCommand(CreateCommand request);

    CommandResult updateCommand(Long commandId, UpdateCommand request);

    PreviewResult preview(PreviewCommand request);

    ValidationResult validate(ValidateCommand request);

    record CreateCommand(
            @NotBlank(message = "type is required")
            String type,
            @Size(max = 20, message = "trigger length must be between 2 and 20")
            String trigger,
            String actionKey,
            @Size(max = 300, message = "messageTemplate length must be 300 or less")
            String messageTemplate,
            @Min(value = 5, message = "timerIntervalMinutes must be between 5 and 1440")
            @Max(value = 1440, message = "timerIntervalMinutes must be between 5 and 1440")
            Integer timerIntervalMinutes,
            @Min(value = 1, message = "timerMinChatCount must be between 1 and 10000")
            @Max(value = 10000, message = "timerMinChatCount must be between 1 and 10000")
            Integer timerMinChatCount,
            Boolean active,
            String requiredRole,
            @Min(value = 0, message = "userCooldownSeconds must be between 0 and 3600")
            @Max(value = 3600, message = "userCooldownSeconds must be between 0 and 3600")
            Integer userCooldownSeconds,
            String actorId
    ) {
    }

    record UpdateCommand(
            String type,
            @Size(max = 20, message = "trigger length must be between 2 and 20")
            String trigger,
            String actionKey,
            @Size(max = 300, message = "messageTemplate length must be 300 or less")
            String messageTemplate,
            @Min(value = 5, message = "timerIntervalMinutes must be between 5 and 1440")
            @Max(value = 1440, message = "timerIntervalMinutes must be between 5 and 1440")
            Integer timerIntervalMinutes,
            @Min(value = 1, message = "timerMinChatCount must be between 1 and 10000")
            @Max(value = 10000, message = "timerMinChatCount must be between 1 and 10000")
            Integer timerMinChatCount,
            Boolean active,
            String requiredRole,
            @Min(value = 0, message = "userCooldownSeconds must be between 0 and 3600")
            @Max(value = 3600, message = "userCooldownSeconds must be between 0 and 3600")
            Integer userCooldownSeconds,
            String actorId
    ) {
    }

    record PreviewCommand(
            @NotBlank(message = "messageTemplate is required")
            @Size(max = 300, message = "messageTemplate length must be 300 or less")
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
            @NotBlank(message = "type is required")
            String type,
            @Size(max = 20, message = "trigger length must be between 2 and 20")
            String trigger,
            String actionKey,
            @Size(max = 300, message = "messageTemplate length must be 300 or less")
            String messageTemplate,
            @Min(value = 5, message = "timerIntervalMinutes must be between 5 and 1440")
            @Max(value = 1440, message = "timerIntervalMinutes must be between 5 and 1440")
            Integer timerIntervalMinutes,
            @Min(value = 1, message = "timerMinChatCount must be between 1 and 10000")
            @Max(value = 10000, message = "timerMinChatCount must be between 1 and 10000")
            Integer timerMinChatCount,
            String requiredRole,
            @Min(value = 0, message = "userCooldownSeconds must be between 0 and 3600")
            @Max(value = 3600, message = "userCooldownSeconds must be between 0 and 3600")
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
