package org.nowstart.nyangnyangbot.application.port.out.command;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.type.CommandActionKey;
import org.nowstart.nyangnyangbot.domain.type.CommandType;

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
            Long id,
            CommandType type,
            String trigger,
            CommandActionKey actionKey,
            String messageTemplate,
            Integer timerIntervalMinutes,
            Integer timerMinChatCount,
            boolean active,
            String requiredRole,
            Integer userCooldownSeconds,
            String createdBy,
            String updatedBy,
            LocalDateTime createDate,
            LocalDateTime modifyDate
    ) {
    }

    record CreateData(
            CommandType type,
            String trigger,
            CommandActionKey actionKey,
            String messageTemplate,
            Integer timerIntervalMinutes,
            Integer timerMinChatCount,
            boolean active,
            String requiredRole,
            Integer userCooldownSeconds,
            String createdBy,
            String updatedBy
    ) {
    }

    record UpdateData(
            Long id,
            String trigger,
            String messageTemplate,
            Integer timerIntervalMinutes,
            Integer timerMinChatCount,
            boolean active,
            String requiredRole,
            Integer userCooldownSeconds,
            String updatedBy
    ) {
    }
}
