package org.nowstart.nyangnyangbot.application.port.out.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundResult;
import org.nowstart.nyangnyangbot.domain.command.CommandExecutionPolicy;

public interface CommandPort {

    List<CommandRecord> findAllOrderByIdDesc();

    Optional<CommandRecord> findByIdForUpdate(Long commandId);

    Optional<CommandRecord> findByTrigger(String trigger);

    Map<String, CommandRecord> findActiveCommandsByTrigger();

    CommandRecord create(CreateData data);

    CommandRecord update(UpdateData data);

    record CommandRecord(
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive")
            Long id,
            @NotBlank(message = "trigger is required")
            String trigger,
            @NotBlank(message = "messageTemplate is required")
            String messageTemplate,
            boolean active,
            @NotNull(message = "executionPolicy is required")
            CommandExecutionPolicy executionPolicy,
            Integer userCooldownSeconds,
            String createdBy,
            String updatedBy,
            LocalDateTime createDate,
            LocalDateTime modifyDate
    ) {
        public CommandRecord(
                Long id,
                String trigger,
                String messageTemplate,
                boolean active,
                Integer userCooldownSeconds,
                String createdBy,
                String updatedBy,
                LocalDateTime createDate,
                LocalDateTime modifyDate
        ) {
            this(id, trigger, messageTemplate, active, CommandExecutionPolicy.USER_INTERVAL,
                    userCooldownSeconds, createdBy, updatedBy, createDate, modifyDate);
        }
    }

    record CreateData(
            @NotBlank(message = "trigger is required")
            String trigger,
            @NotBlank(message = "messageTemplate is required")
            String messageTemplate,
            boolean active,
            @NotNull(message = "executionPolicy is required")
            CommandExecutionPolicy executionPolicy,
            Integer userCooldownSeconds,
            String createdBy,
            String updatedBy
    ) {
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
            String trigger,
            @NotBlank(message = "messageTemplate is required")
            String messageTemplate,
            boolean active,
            @NotNull(message = "executionPolicy is required")
            CommandExecutionPolicy executionPolicy,
            Integer userCooldownSeconds,
            String updatedBy
    ) {
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
