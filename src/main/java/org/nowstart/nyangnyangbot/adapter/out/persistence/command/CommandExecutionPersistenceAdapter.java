package org.nowstart.nyangnyangbot.adapter.out.persistence.command;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.entity.Command;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.entity.CommandExecution;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.repository.CommandExecutionRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.repository.CommandRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.UserAccountRepository;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandExecutionPort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommandExecutionPersistenceAdapter implements CommandExecutionPort {

    private final CommandRepository commandRepository;
    private final CommandExecutionRepository executionRepository;
    private final UserAccountRepository userAccountRepository;

    @Override
    public Optional<LockedCommand> lockActiveCommand(String normalizedTrigger) {
        return commandRepository.findActiveByTriggerForUpdate(normalizedTrigger).map(this::lockedCommand);
    }

    @Override
    public void observeAndLockUser(String userId, String displayName) {
        userAccountRepository.observe(userId, displayName);
        userAccountRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("Observed user account was not found"));
    }

    @Override
    public Instant currentDatabaseTime() {
        return userAccountRepository.currentDatabaseTime();
    }

    @Override
    public Optional<ExecutionRecord> findLatestForUpdate(long commandId, String userId) {
        return executionRepository
                .findFirstByCommandIdAndUserAccountUserIdOrderByExecutedAtDescIdDesc(commandId, userId)
                .map(this::executionRecord);
    }

    @Override
    public boolean existsCalendarDayStartedAt(long commandId, String userId, Instant calendarDayStartedAt) {
        return executionRepository.existsByCommandIdAndUserAccountUserIdAndCalendarDayStartedAt(
                commandId,
                userId,
                calendarDayStartedAt
        );
    }

    @Override
    public void append(ExecutionData data) {
        Command command = commandRepository.getReferenceById(data.commandId());
        UserAccount user = userAccountRepository.getReferenceById(data.userId());
        executionRepository.saveAndFlush(CommandExecution.builder()
                .command(command)
                .userAccount(user)
                .executedAt(data.executedAt())
                .executionPolicySnapshot(data.executionPolicy())
                .cooldownSecondsSnapshot(data.cooldownSeconds())
                .calendarDayStartedAt(data.calendarDayStartedAt())
                .build());
    }

    @Override
    public long countAll(long commandId) {
        return executionRepository.countByCommandId(commandId);
    }

    @Override
    public long countForUser(long commandId, String userId) {
        return executionRepository.countByCommandIdAndUserAccountUserId(commandId, userId);
    }

    @Override
    public List<Instant> findCalendarDayStarts(long commandId, String userId) {
        return executionRepository.findCalendarDayStarts(commandId, userId);
    }

    private LockedCommand lockedCommand(Command command) {
        return new LockedCommand(
                command.getId(),
                command.getTriggerToken(),
                command.getMessageTemplate(),
                command.getExecutionPolicy(),
                command.getUserCooldownSeconds()
        );
    }

    private ExecutionRecord executionRecord(CommandExecution execution) {
        return new ExecutionRecord(execution.getExecutedAt());
    }
}
