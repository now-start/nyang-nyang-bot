package org.nowstart.nyangnyangbot.adapter.out.persistence.command;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.entity.Command;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.repository.CommandRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.UserAccountRepository;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort;
import org.nowstart.nyangnyangbot.config.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommandPersistenceAdapter implements CommandPort {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final CommandRepository commandRepository;
    private final UserAccountRepository userAccountRepository;
    private final OutboundContractValidator contractValidator;

    @Override
    public List<CommandRecord> findAllOrderByIdDesc() {
        return commandRepository.findAllByOrderByIdDesc().stream()
                .map(this::commandRecord)
                .toList();
    }

    @Override
    public Optional<CommandRecord> findByIdForUpdate(Long commandId) {
        return commandRepository.findByIdForUpdate(commandId).map(this::commandRecord);
    }

    @Override
    public Optional<CommandRecord> findByTrigger(String trigger) {
        return commandRepository.findByTriggerToken(trigger).map(this::commandRecord);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.COMMAND_ACTIVE_BY_TRIGGER, sync = true)
    public Map<String, CommandRecord> findActiveCommandsByTrigger() {
        return commandRepository.findByActiveTrue().stream()
                .map(this::commandRecord)
                .collect(toUnmodifiableMap(CommandRecord::trigger, identity()));
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.COMMAND_ACTIVE_BY_TRIGGER, allEntries = true)
    public CommandRecord create(CreateData data) {
        contractValidator.request("command.create", data);
        Command saved = commandRepository.save(Command.builder()
                .triggerToken(data.trigger())
                .messageTemplate(data.messageTemplate())
                .active(data.active())
                .executionPolicy(data.executionPolicy())
                .userCooldownSeconds(data.userCooldownSeconds())
                .createdByUser(actorReference(data.createdBy()))
                .updatedByUser(actorReference(data.updatedBy()))
                .build());
        return commandRecord(saved);
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.COMMAND_ACTIVE_BY_TRIGGER, allEntries = true)
    public CommandRecord update(UpdateData data) {
        contractValidator.request("command.update", data);
        Command command = commandRepository.findByIdForUpdate(data.id())
                .orElseThrow(() -> new IllegalArgumentException("command not found"));
        command.update(
                data.trigger(),
                data.messageTemplate(),
                data.active(),
                data.executionPolicy(),
                data.userCooldownSeconds(),
                actorReference(data.updatedBy())
        );
        return commandRecord(command);
    }

    private CommandRecord commandRecord(Command entity) {
        return contractValidator.persistenceResult("command.commandRecord", new CommandRecord(
                entity.getId(),
                entity.getTriggerToken(),
                entity.getMessageTemplate(),
                entity.isActive(),
                entity.getExecutionPolicy(),
                entity.getUserCooldownSeconds(),
                userId(entity.getCreatedByUser()),
                userId(entity.getUpdatedByUser()),
                localDateTime(entity.getCreatedAt()),
                localDateTime(entity.getUpdatedAt())
        ));
    }

    private UserAccount actorReference(String userId) {
        if (userId == null || userId.isBlank() || "system".equals(userId)) {
            return null;
        }
        return userAccountRepository.getReferenceById(userId);
    }

    private String userId(UserAccount account) {
        return account == null ? null : account.getUserId();
    }

    private LocalDateTime localDateTime(java.time.Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, SEOUL);
    }
}
