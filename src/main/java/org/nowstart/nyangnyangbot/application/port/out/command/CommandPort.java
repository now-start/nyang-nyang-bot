package org.nowstart.nyangnyangbot.application.port.out.command;

import static org.nowstart.nyangnyangbot.domain.command.CommandPolicy.TEMPLATE_LENGTH_MESSAGE;
import static org.nowstart.nyangnyangbot.domain.command.CommandPolicy.USER_COOLDOWN_RANGE_MESSAGE;

import jakarta.validation.Valid;
import static org.nowstart.nyangnyangbot.domain.command.CommandPolicy.MAX_TEMPLATE_LENGTH;
import static org.nowstart.nyangnyangbot.domain.command.CommandPolicy.MAX_USER_COOLDOWN_SECONDS;
import static org.nowstart.nyangnyangbot.domain.command.CommandPolicy.MIN_USER_COOLDOWN_SECONDS;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.chat.CommandTrigger;
import org.nowstart.nyangnyangbot.domain.command.CommandExecutionPolicy;

public interface CommandPort {

    /** 모든 명령어를 식별자 내림차순으로 반환한다. */
    @Valid
    List<CommandRecord> findAllOrderByIdDesc();

    /** 식별자로 명령어를 조회하고 현재 트랜잭션 동안 쓰기 잠금을 유지한다. */
    @Valid
    Optional<CommandRecord> findByIdForUpdate(Long commandId);

    /** 정규화된 트리거로 명령어를 조회한다. */
    @Valid
    Optional<CommandRecord> findByTrigger(String trigger);

    /** 활성 명령어를 정규화된 트리거를 키로 하는 맵으로 반환한다. */
    @Valid
    Map<String, CommandRecord> findActiveCommandsByTrigger();

    /** 새 명령어를 저장하고 저장된 결과를 반환한다. */
    @Valid
    CommandRecord create(@Valid @NotNull(message = "command data is required") CreateData data);

    /** 기존 명령어의 변경 사항을 저장하고 저장된 결과를 반환한다. */
    @Valid
    CommandRecord update(@Valid @NotNull(message = "command data is required") UpdateData data);

    record CommandRecord(
            @NotNull(message = "id is required")
            @Positive(message = "id must be positive")
            Long id,
            @NotBlank(message = "trigger is required")
            @Size(min = CommandTrigger.MIN_LENGTH, max = CommandTrigger.MAX_LENGTH,
                    message = CommandTrigger.LENGTH_MESSAGE)
            String trigger,
            @NotBlank(message = "messageTemplate is required")
            @Size(max = MAX_TEMPLATE_LENGTH, message = TEMPLATE_LENGTH_MESSAGE)
            String messageTemplate,
            boolean active,
            @NotNull(message = "executionPolicy is required")
            CommandExecutionPolicy executionPolicy,
            @Min(value = MIN_USER_COOLDOWN_SECONDS, message = USER_COOLDOWN_RANGE_MESSAGE)
            @Max(value = MAX_USER_COOLDOWN_SECONDS, message = USER_COOLDOWN_RANGE_MESSAGE)
            Integer userCooldownSeconds,
            String createdBy,
            String updatedBy
    ) {
        @AssertTrue(message = "userCooldownSeconds is required for interval policy")
        public boolean hasRequiredCooldown() {
            return executionPolicy != CommandExecutionPolicy.USER_INTERVAL || userCooldownSeconds != null;
        }

        public CommandRecord(
                Long id,
                String trigger,
                String messageTemplate,
                boolean active,
                Integer userCooldownSeconds,
                String createdBy,
                String updatedBy
        ) {
            this(id, trigger, messageTemplate, active, CommandExecutionPolicy.USER_INTERVAL,
                    userCooldownSeconds, createdBy, updatedBy);
        }
    }

    record CreateData(
            @NotBlank(message = "trigger is required")
            @Size(min = CommandTrigger.MIN_LENGTH, max = CommandTrigger.MAX_LENGTH,
                    message = CommandTrigger.LENGTH_MESSAGE)
            String trigger,
            @NotBlank(message = "messageTemplate is required")
            @Size(max = MAX_TEMPLATE_LENGTH, message = TEMPLATE_LENGTH_MESSAGE)
            String messageTemplate,
            boolean active,
            @NotNull(message = "executionPolicy is required")
            CommandExecutionPolicy executionPolicy,
            @Min(value = MIN_USER_COOLDOWN_SECONDS, message = USER_COOLDOWN_RANGE_MESSAGE)
            @Max(value = MAX_USER_COOLDOWN_SECONDS, message = USER_COOLDOWN_RANGE_MESSAGE)
            Integer userCooldownSeconds,
            String createdBy,
            String updatedBy
    ) {
        @AssertTrue(message = "userCooldownSeconds is required for interval policy")
        public boolean hasRequiredCooldown() {
            return executionPolicy != CommandExecutionPolicy.USER_INTERVAL || userCooldownSeconds != null;
        }

        public CreateData(
                String trigger,
                String messageTemplate,
                boolean active,
                Integer userCooldownSeconds,
                String createdBy,
                String updatedBy
        ) {
            this(trigger, messageTemplate, active, CommandExecutionPolicy.USER_INTERVAL,
                    userCooldownSeconds, createdBy, updatedBy);
        }
    }

    record UpdateData(
            @NotNull(message = "id is required")
            @Positive(message = "id must be positive")
            Long id,
            @NotBlank(message = "trigger is required")
            @Size(min = CommandTrigger.MIN_LENGTH, max = CommandTrigger.MAX_LENGTH,
                    message = CommandTrigger.LENGTH_MESSAGE)
            String trigger,
            @NotBlank(message = "messageTemplate is required")
            @Size(max = MAX_TEMPLATE_LENGTH, message = TEMPLATE_LENGTH_MESSAGE)
            String messageTemplate,
            boolean active,
            @NotNull(message = "executionPolicy is required")
            CommandExecutionPolicy executionPolicy,
            @Min(value = MIN_USER_COOLDOWN_SECONDS, message = USER_COOLDOWN_RANGE_MESSAGE)
            @Max(value = MAX_USER_COOLDOWN_SECONDS, message = USER_COOLDOWN_RANGE_MESSAGE)
            Integer userCooldownSeconds,
            String updatedBy
    ) {
        @AssertTrue(message = "userCooldownSeconds is required for interval policy")
        public boolean hasRequiredCooldown() {
            return executionPolicy != CommandExecutionPolicy.USER_INTERVAL || userCooldownSeconds != null;
        }

        public UpdateData(
                Long id,
                String trigger,
                String messageTemplate,
                boolean active,
                Integer userCooldownSeconds,
                String updatedBy
        ) {
            this(id, trigger, messageTemplate, active, CommandExecutionPolicy.USER_INTERVAL,
                    userCooldownSeconds, updatedBy);
        }
    }
}
