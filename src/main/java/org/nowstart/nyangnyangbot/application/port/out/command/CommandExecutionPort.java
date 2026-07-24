package org.nowstart.nyangnyangbot.application.port.out.command;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.command.CommandExecutionPolicy;

public interface CommandExecutionPort {

    Optional<LockedCommand> lockActiveCommand(String normalizedTrigger);

    void observeAndLockUser(String userId, String displayName);

    Instant currentDatabaseTime();

    Optional<ExecutionRecord> findLatestForUpdate(long commandId, String userId);

    boolean existsCalendarDayStartedAt(long commandId, String userId, Instant calendarDayStartedAt);

    void append(ExecutionData data);

    long countAll(long commandId);

    long countForUser(long commandId, String userId);

    List<Instant> findCalendarDayStarts(long commandId, String userId);

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
            Instant calendarDayStartedAt
    ) {
    }
}
