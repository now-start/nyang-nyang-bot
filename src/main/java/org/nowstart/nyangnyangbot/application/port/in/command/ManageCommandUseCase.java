package org.nowstart.nyangnyangbot.application.port.in.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.nowstart.nyangnyangbot.domain.chat.CommandTrigger;
import org.nowstart.nyangnyangbot.domain.command.CommandExecutionPolicy;

public interface ManageCommandUseCase {

    int MAX_TEMPLATE_LENGTH = 1_000;
    int MIN_TRIGGER_LENGTH = CommandTrigger.MIN_LENGTH;
    int MAX_TRIGGER_LENGTH = CommandTrigger.MAX_LENGTH;
    CommandExecutionPolicy DEFAULT_EXECUTION_POLICY = CommandExecutionPolicy.USER_INTERVAL;
    CommandExecutionPolicy CALENDAR_DAY_EXECUTION_POLICY = CommandExecutionPolicy.USER_CALENDAR_DAY;
    int DEFAULT_USER_COOLDOWN_SECONDS = 30;
    int MIN_USER_COOLDOWN_SECONDS = 5;
    int MAX_USER_COOLDOWN_SECONDS = 3_600;
    String TEMPLATE_LENGTH_MESSAGE =
            "messageTemplate length must be " + MAX_TEMPLATE_LENGTH + " or less";
    String TRIGGER_LENGTH_MESSAGE = CommandTrigger.LENGTH_MESSAGE;
    String USER_COOLDOWN_RANGE_MESSAGE = "userCooldownSeconds must be between "
            + MIN_USER_COOLDOWN_SECONDS + " and " + MAX_USER_COOLDOWN_SECONDS;

    static CommandExecutionPolicy executionPolicy(String value) {
        return value == null || value.isBlank()
                ? DEFAULT_EXECUTION_POLICY
                : CommandExecutionPolicy.valueOf(value);
    }

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
            @Size(min = MIN_TRIGGER_LENGTH, max = MAX_TRIGGER_LENGTH, message = TRIGGER_LENGTH_MESSAGE)
            String trigger,
            @NotBlank(message = "messageTemplate is required")
            @Size(max = MAX_TEMPLATE_LENGTH, message = TEMPLATE_LENGTH_MESSAGE)
            String messageTemplate,
            Boolean active,
            CommandExecutionPolicy executionPolicy,
            @Min(value = MIN_USER_COOLDOWN_SECONDS,
                    message = USER_COOLDOWN_RANGE_MESSAGE)
            @Max(value = MAX_USER_COOLDOWN_SECONDS,
                    message = USER_COOLDOWN_RANGE_MESSAGE)
            Integer userCooldownSeconds,
            String actorId
    ) {
        public CreateCommand(
                String trigger,
                String messageTemplate,
                Boolean active,
                Integer userCooldownSeconds,
                String actorId
        ) {
            this(trigger, messageTemplate, active, DEFAULT_EXECUTION_POLICY,
                    userCooldownSeconds, actorId);
        }
    }

    record UpdateCommand(
            @Size(min = MIN_TRIGGER_LENGTH, max = MAX_TRIGGER_LENGTH, message = TRIGGER_LENGTH_MESSAGE)
            String trigger,
            @Size(max = MAX_TEMPLATE_LENGTH, message = TEMPLATE_LENGTH_MESSAGE)
            String messageTemplate,
            Boolean active,
            CommandExecutionPolicy executionPolicy,
            @Min(value = MIN_USER_COOLDOWN_SECONDS,
                    message = USER_COOLDOWN_RANGE_MESSAGE)
            @Max(value = MAX_USER_COOLDOWN_SECONDS,
                    message = USER_COOLDOWN_RANGE_MESSAGE)
            Integer userCooldownSeconds,
            String actorId
    ) {
        public UpdateCommand(
                String trigger,
                String messageTemplate,
                Boolean active,
                Integer userCooldownSeconds,
                String actorId
        ) {
            this(trigger, messageTemplate, active, null, userCooldownSeconds, actorId);
        }
    }

    record PreviewCommand(
            @NotBlank(message = "messageTemplate is required")
            @Size(max = MAX_TEMPLATE_LENGTH, message = TEMPLATE_LENGTH_MESSAGE)
            String messageTemplate
    ) {
    }

    record ValidateCommand(
            Long commandId,
            @NotBlank(message = "trigger is required")
            @Size(min = MIN_TRIGGER_LENGTH, max = MAX_TRIGGER_LENGTH, message = TRIGGER_LENGTH_MESSAGE)
            String trigger,
            @NotBlank(message = "messageTemplate is required")
            @Size(max = MAX_TEMPLATE_LENGTH, message = TEMPLATE_LENGTH_MESSAGE)
            String messageTemplate,
            CommandExecutionPolicy executionPolicy,
            @Min(value = MIN_USER_COOLDOWN_SECONDS,
                    message = USER_COOLDOWN_RANGE_MESSAGE)
            @Max(value = MAX_USER_COOLDOWN_SECONDS,
                    message = USER_COOLDOWN_RANGE_MESSAGE)
            Integer userCooldownSeconds
    ) {
        public ValidateCommand(
                Long commandId,
                String trigger,
                String messageTemplate,
                Integer userCooldownSeconds
        ) {
            this(commandId, trigger, messageTemplate, DEFAULT_EXECUTION_POLICY, userCooldownSeconds);
        }
    }

    record CommandResult(
            Long id,
            String trigger,
            String messageTemplate,
            boolean active,
            CommandExecutionPolicy executionPolicy,
            Integer userCooldownSeconds,
            String createdBy,
            String updatedBy
    ) {
        public CommandResult(
                Long id,
                String trigger,
                String messageTemplate,
                boolean active,
                Integer userCooldownSeconds,
                String createdBy,
                String updatedBy
        ) {
            this(id, trigger, messageTemplate, active, DEFAULT_EXECUTION_POLICY,
                    userCooldownSeconds, createdBy, updatedBy);
        }
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
