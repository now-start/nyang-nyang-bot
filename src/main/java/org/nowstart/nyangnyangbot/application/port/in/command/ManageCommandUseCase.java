package org.nowstart.nyangnyangbot.application.port.in.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.nowstart.nyangnyangbot.domain.chat.CommandTrigger;
import org.nowstart.nyangnyangbot.domain.command.CommandExecutionPolicy;
import org.nowstart.nyangnyangbot.domain.command.CommandPolicy;

public interface ManageCommandUseCase {

    int MAX_TEMPLATE_LENGTH = CommandPolicy.MAX_TEMPLATE_LENGTH;
    int MAX_TRIGGER_LENGTH = CommandTrigger.MAX_LENGTH;
    String TRIGGER_LENGTH_MESSAGE = CommandTrigger.LENGTH_MESSAGE;
    CommandExecutionPolicy DEFAULT_EXECUTION_POLICY = CommandPolicy.DEFAULT_EXECUTION_POLICY;
    CommandExecutionPolicy CALENDAR_DAY_EXECUTION_POLICY = CommandExecutionPolicy.USER_CALENDAR_DAY;
    int DEFAULT_USER_COOLDOWN_SECONDS = CommandPolicy.DEFAULT_USER_COOLDOWN_SECONDS;
    int MIN_USER_COOLDOWN_SECONDS = CommandPolicy.MIN_USER_COOLDOWN_SECONDS;
    int MAX_USER_COOLDOWN_SECONDS = CommandPolicy.MAX_USER_COOLDOWN_SECONDS;
    String TEMPLATE_LENGTH_MESSAGE = CommandPolicy.TEMPLATE_LENGTH_MESSAGE;
    String USER_COOLDOWN_RANGE_MESSAGE = CommandPolicy.USER_COOLDOWN_RANGE_MESSAGE;

    /** 요청 값을 실행 정책으로 변환하며, 값이 없으면 기본 정책을 사용한다. */
    static CommandExecutionPolicy executionPolicy(String value) {
        return value == null || value.isBlank()
                ? DEFAULT_EXECUTION_POLICY
                : CommandExecutionPolicy.valueOf(value);
    }

    /** 설정된 모든 명령어를 관리 화면 표시 순서로 반환한다. */
    List<CommandResult> getCommands();

    /** 명령어 메시지 템플릿에서 사용할 수 있는 변수를 반환한다. */
    List<VariableResult> getVariables();

    /** 검증된 관리 요청으로 명령어를 생성한다. */
    CommandResult createCommand(@Valid @NotNull(message = "command is required") CreateCommand request);

    /** 지정한 명령어를 전달된 필드로 수정한다. */
    CommandResult updateCommand(
            @NotNull(message = "commandId is required")
            @Positive(message = "commandId must be positive") Long commandId,
            @Valid @NotNull(message = "command is required") UpdateCommand request
    );

    /** 전체 명령어 입력을 검증하고 저장하거나 실행하지 않은 채 메시지 템플릿을 렌더링한다. */
    PreviewResult preview(@Valid @NotNull(message = "preview is required") PreviewCommand request);

    record CreateCommand(
            @NotBlank(message = "trigger is required")
            @Size(min = CommandTrigger.MIN_LENGTH, max = MAX_TRIGGER_LENGTH, message = CommandTrigger.LENGTH_MESSAGE)
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
            @Size(min = CommandTrigger.MIN_LENGTH, max = MAX_TRIGGER_LENGTH, message = CommandTrigger.LENGTH_MESSAGE)
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
            Long commandId,
            @NotBlank(message = "trigger is required")
            @Size(min = CommandTrigger.MIN_LENGTH, max = MAX_TRIGGER_LENGTH, message = CommandTrigger.LENGTH_MESSAGE)
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
        public PreviewCommand(
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
}
