package org.nowstart.nyangnyangbot.application.port.out.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.type.CommandActionKey;
import org.nowstart.nyangnyangbot.domain.type.CommandType;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundResult;

public interface CommandPort {

    List<CommandRecord> findAllOrderByIdDesc();

    Optional<CommandRecord> findById(Long commandId);

    Optional<CommandRecord> findByTrigger(String trigger);

    Optional<CommandRecord> findByActionKey(CommandActionKey actionKey);

    Optional<CommandRecord> findActiveByTrigger(String trigger);

    Optional<CommandRecord> findActiveByActionKey(CommandActionKey actionKey);

    CommandRecord create(CreateData data);

    CommandRecord update(UpdateData data);

    record CommandRecord(
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive")
            Long id,
            @NotNull(message = "type is required")
            CommandType type,
            String trigger,
            CommandActionKey actionKey,
            String messageTemplate,
            Integer timerIntervalMinutes,
            Integer timerMinChatCount,
            boolean active,
            @NotBlank(message = "requiredRole is required")
            String requiredRole,
            @NotNull(message = "userCooldownSeconds is required")
            @PositiveOrZero(message = "userCooldownSeconds must not be negative")
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
            @NotNull(message = "type is required")
            CommandType type,
            String trigger,
            CommandActionKey actionKey,
            String messageTemplate,
            Integer timerIntervalMinutes,
            Integer timerMinChatCount,
            boolean active,
            @NotBlank(message = "requiredRole is required")
            String requiredRole,
            @NotNull(message = "userCooldownSeconds is required")
            @PositiveOrZero(message = "userCooldownSeconds must not be negative")
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
            String trigger,
            String messageTemplate,
            Integer timerIntervalMinutes,
            Integer timerMinChatCount,
            boolean active,
            @NotBlank(message = "requiredRole is required")
            String requiredRole,
            @NotNull(message = "userCooldownSeconds is required")
            @PositiveOrZero(message = "userCooldownSeconds must not be negative")
            Integer userCooldownSeconds,
            @NotBlank(message = "updatedBy is required")
            String updatedBy
    ) {
    }
}
