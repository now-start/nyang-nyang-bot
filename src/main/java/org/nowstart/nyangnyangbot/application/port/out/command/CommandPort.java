package org.nowstart.nyangnyangbot.application.port.out.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundResult;

public interface CommandPort {

    List<CommandRecord> findAllOrderByIdDesc();

    Optional<CommandRecord> findById(Long commandId);

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
            @NotNull(message = "userCooldownSeconds is required")
            @Positive(message = "userCooldownSeconds must be positive")
            Integer userCooldownSeconds,
            @NotBlank(message = "createdBy is required")
            String createdBy,
            @NotBlank(message = "updatedBy is required")
            String updatedBy,
            LocalDateTime createDate,
            LocalDateTime modifyDate
    ) {
    }

    record CreateData(
            @NotBlank(message = "trigger is required")
            String trigger,
            @NotBlank(message = "messageTemplate is required")
            String messageTemplate,
            boolean active,
            @NotNull(message = "userCooldownSeconds is required")
            @Positive(message = "userCooldownSeconds must be positive")
            Integer userCooldownSeconds,
            @NotBlank(message = "createdBy is required")
            String createdBy,
            @NotBlank(message = "updatedBy is required")
            String updatedBy
    ) {
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
            @NotNull(message = "userCooldownSeconds is required")
            @Positive(message = "userCooldownSeconds must be positive")
            Integer userCooldownSeconds,
            @NotBlank(message = "updatedBy is required")
            String updatedBy
    ) {
    }
}
