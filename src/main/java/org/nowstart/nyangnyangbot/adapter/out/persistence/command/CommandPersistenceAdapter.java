package org.nowstart.nyangnyangbot.adapter.out.persistence.command;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.entity.Command;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.repository.CommandRepository;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort;
import org.nowstart.nyangnyangbot.config.cache.CacheNames;
import org.nowstart.nyangnyangbot.domain.type.CommandActionKey;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommandPersistenceAdapter implements CommandPort {

    private final CommandRepository commandRepository;
    private final CommandPersistenceMapper mapper;

    @Override
    public List<CommandRecord> findAllOrderByIdDesc() {
        return commandRepository.findAllByOrderByIdDesc().stream()
                .map(mapper::commandRecord)
                .toList();
    }

    @Override
    public Optional<CommandRecord> findById(Long commandId) {
        return commandRepository.findById(commandId).map(mapper::commandRecord);
    }

    @Override
    public Optional<CommandRecord> findByTrigger(String trigger) {
        return commandRepository.findByTriggerToken(trigger).map(mapper::commandRecord);
    }

    @Override
    public Optional<CommandRecord> findByActionKey(CommandActionKey actionKey) {
        return commandRepository.findByActionKey(actionKey).map(mapper::commandRecord);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.COMMAND_ACTIVE_BY_TRIGGER, key = "#trigger", unless = "#result == null")
    public Optional<CommandRecord> findActiveByTrigger(String trigger) {
        return commandRepository.findByTriggerTokenAndActiveTrue(trigger).map(mapper::commandRecord);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.COMMAND_ACTIVE_BY_ACTION_KEY, key = "#actionKey", unless = "#result == null")
    public Optional<CommandRecord> findActiveByActionKey(CommandActionKey actionKey) {
        return commandRepository.findByActionKeyAndActiveTrue(actionKey).map(mapper::commandRecord);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.COMMAND_ACTIVE_BY_TRIGGER, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COMMAND_ACTIVE_BY_ACTION_KEY, allEntries = true)
    })
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
        return mapper.commandRecord(saved);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.COMMAND_ACTIVE_BY_TRIGGER, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.COMMAND_ACTIVE_BY_ACTION_KEY, allEntries = true)
    })
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
        return mapper.commandRecord(command);
    }
}
