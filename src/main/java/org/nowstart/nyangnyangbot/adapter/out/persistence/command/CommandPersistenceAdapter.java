package org.nowstart.nyangnyangbot.adapter.out.persistence.command;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.entity.Command;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.repository.CommandRepository;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.CommandRecord;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.CreateData;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.UpdateData;
import org.nowstart.nyangnyangbot.domain.type.CommandActionKey;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommandPersistenceAdapter implements CommandPort {

    private final CommandRepository commandRepository;

    @Override
    public List<CommandRecord> findAllOrderByIdDesc() {
        return commandRepository.findAllByOrderByIdDesc().stream()
                .map(this::commandRecord)
                .toList();
    }

    @Override
    public Optional<CommandRecord> findById(Long commandId) {
        return commandRepository.findById(commandId).map(this::commandRecord);
    }

    @Override
    public Optional<CommandRecord> findByTrigger(String trigger) {
        return commandRepository.findByTriggerToken(trigger).map(this::commandRecord);
    }

    @Override
    public Optional<CommandRecord> findByActionKey(CommandActionKey actionKey) {
        return commandRepository.findByActionKey(actionKey).map(this::commandRecord);
    }

    @Override
    public Optional<CommandRecord> findActiveByTrigger(String trigger) {
        return commandRepository.findByTriggerTokenAndActiveTrue(trigger).map(this::commandRecord);
    }

    @Override
    public Optional<CommandRecord> findActiveByActionKey(CommandActionKey actionKey) {
        return commandRepository.findByActionKeyAndActiveTrue(actionKey).map(this::commandRecord);
    }

    @Override
    public CommandRecord create(CreateData data) {
        Command saved = commandRepository.save(Command.builder()
                .type(data.type())
                .triggerToken(data.trigger())
                .actionKey(data.actionKey())
                .messageTemplate(data.messageTemplate())
                .timerIntervalMinutes(data.timerIntervalMinutes())
                .timerMinChatCount(data.timerMinChatCount())
                .active(data.active())
                .requiredRole(data.requiredRole())
                .userCooldownSeconds(data.userCooldownSeconds())
                .createdBy(data.createdBy())
                .updatedBy(data.updatedBy())
                .build());
        return commandRecord(saved);
    }

    @Override
    public CommandRecord update(UpdateData data) {
        Command command = commandRepository.findById(data.id())
                .orElseThrow(() -> new IllegalArgumentException("command not found"));
        command.update(
                data.trigger(),
                data.messageTemplate(),
                data.timerIntervalMinutes(),
                data.timerMinChatCount(),
                data.active(),
                data.requiredRole(),
                data.userCooldownSeconds(),
                data.updatedBy()
        );
        return commandRecord(command);
    }

    private CommandRecord commandRecord(Command entity) {
        return new CommandRecord(
                entity.getId(),
                entity.getType(),
                entity.getTriggerToken(),
                entity.getActionKey(),
                entity.getMessageTemplate(),
                entity.getTimerIntervalMinutes(),
                entity.getTimerMinChatCount(),
                entity.isActive(),
                entity.getRequiredRole(),
                entity.getUserCooldownSeconds(),
                entity.getCreatedBy(),
                entity.getUpdatedBy(),
                entity.getCreateDate(),
                entity.getModifyDate()
        );
    }
}
