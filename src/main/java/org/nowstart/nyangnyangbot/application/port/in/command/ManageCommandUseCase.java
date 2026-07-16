package org.nowstart.nyangnyangbot.application.port.in.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public interface ManageCommandUseCase {

    int MAX_TEMPLATE_LENGTH = 1_000;

    List<CommandResult> getCommands();

    List<VariableResult> getVariables();

    CommandResult createCommand(@Valid @NotNull(message = "command is required") CreateCommand request);

    CommandResult updateCommand(
            Long commandId,
            @Valid @NotNull(message = "command is required") UpdateCommand request
    );

    PreviewResult preview(@Valid @NotNull(message = "preview is required") PreviewCommand request);

    ValidationResult validate(ValidateCommand request);

    record CreateCommand(
            @NotBlank(message = "trigger is required")
            @Size(min = 2, max = 20, message = "trigger length must be between 2 and 20")
            String trigger,
            @NotBlank(message = "messageTemplate is required")
            @Size(max = MAX_TEMPLATE_LENGTH, message = "messageTemplate length must be 1000 or less")
            String messageTemplate,
            Boolean active,
            @Min(value = 5, message = "userCooldownSeconds must be between 5 and 3600")
            @Max(value = 3600, message = "userCooldownSeconds must be between 5 and 3600")
            Integer userCooldownSeconds,
            String actorId
    ) {
    }

    record UpdateCommand(
            @Size(min = 2, max = 20, message = "trigger length must be between 2 and 20")
            String trigger,
            @Size(max = MAX_TEMPLATE_LENGTH, message = "messageTemplate length must be 1000 or less")
            String messageTemplate,
            Boolean active,
            @Min(value = 5, message = "userCooldownSeconds must be between 5 and 3600")
            @Max(value = 3600, message = "userCooldownSeconds must be between 5 and 3600")
            Integer userCooldownSeconds,
            String actorId
    ) {
    }

    record PreviewCommand(
            @NotBlank(message = "messageTemplate is required")
            @Size(max = MAX_TEMPLATE_LENGTH, message = "messageTemplate length must be 1000 or less")
            String messageTemplate
    ) {
    }

    record ValidateCommand(
            Long commandId,
            @NotBlank(message = "trigger is required")
            @Size(min = 2, max = 20, message = "trigger length must be between 2 and 20")
            String trigger,
            @NotBlank(message = "messageTemplate is required")
            @Size(max = MAX_TEMPLATE_LENGTH, message = "messageTemplate length must be 1000 or less")
            String messageTemplate,
            @Min(value = 5, message = "userCooldownSeconds must be between 5 and 3600")
            @Max(value = 3600, message = "userCooldownSeconds must be between 5 and 3600")
            Integer userCooldownSeconds
    ) {
    }

    record CommandResult(
            Long id,
            String trigger,
            String messageTemplate,
            boolean active,
            Integer userCooldownSeconds,
            String createdBy,
            String updatedBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    record VariableResult(
            String key,
            String label,
            String description,
            String example
    ) {
    }

    record PreviewResult(String message) {
    }

    record ValidationResult(boolean valid, List<String> errors) {
    }
}
