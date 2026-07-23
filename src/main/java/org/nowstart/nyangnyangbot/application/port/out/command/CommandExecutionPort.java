package org.nowstart.nyangnyangbot.application.port.out.command;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.command.CommandExecutionPolicy;

public interface CommandExecutionPort {

    Optional<LockedCommand> lockActiveCommand(String normalizedTrigger);

    void observeAndLockUser(String userId, String displayName);

    Instant currentDatabaseTime();

    Optional<ExecutionRecord> findLatestForUpdate(long commandId, String userId);

    boolean existsCalendarDate(long commandId, String userId, LocalDate calendarDate);

    void append(ExecutionData data);

    long countAll(long commandId);

    long countForUser(long commandId, String userId);

    List<LocalDate> findExecutionDates(long commandId, String userId);

    record LockedCommand(
            long id,
            String trigger,
            String messageTemplate,
            CommandExecutionPolicy executionPolicy,
            Integer userCooldownSeconds
    ) {
    }

    record ExecutionRecord(
            Instant executedAt
    ) {
    }

    record ExecutionData(
            long commandId,
            String userId,
            Instant executedAt,
            CommandExecutionPolicy executionPolicy,
            Integer cooldownSeconds,
            LocalDate calendarDate
    ) {
    }
}
