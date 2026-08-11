package org.nowstart.nyangnyangbot.application.port.out.command;

import static org.nowstart.nyangnyangbot.application.validation.CommandValidationMessages.TEMPLATE_LENGTH_MESSAGE;
import static org.nowstart.nyangnyangbot.application.validation.CommandValidationMessages.USER_COOLDOWN_RANGE_MESSAGE;
import static org.nowstart.nyangnyangbot.domain.command.CommandPolicy.MAX_TEMPLATE_LENGTH;
import static org.nowstart.nyangnyangbot.domain.command.CommandPolicy.MAX_USER_COOLDOWN_SECONDS;
import static org.nowstart.nyangnyangbot.domain.command.CommandPolicy.MIN_USER_COOLDOWN_SECONDS;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.chat.CommandTrigger;
import org.nowstart.nyangnyangbot.domain.command.CommandExecutionPolicy;

public interface CommandExecutionPort {

    Optional<LockedCommand> lockActiveCommand(String normalizedTrigger);

    void observeAndLockUser(ObserveUserCommand command);

    Instant currentDatabaseTime();

    Optional<ExecutionRecord> findLatestForUpdate(long commandId, String userId);

    boolean existsCalendarDayStartedAt(long commandId, String userId, Instant calendarDayStartedAt);

    void append(ExecutionData data);

    long countAll(long commandId);

    long countForUser(long commandId, String userId);

    List<Instant> findCalendarDayStarts(long commandId, String userId);

    record LockedCommand(
            @Positive(message = "id must be positive") long id,
            @NotBlank(message = "trigger is required")
            @Size(min = CommandTrigger.MIN_LENGTH, max = CommandTrigger.MAX_LENGTH,
                    message = CommandTrigger.LENGTH_MESSAGE) String trigger,
            @NotBlank(message = "messageTemplate is required")
            @Size(max = MAX_TEMPLATE_LENGTH, message = TEMPLATE_LENGTH_MESSAGE) String messageTemplate,
            @NotNull(message = "executionPolicy is required") CommandExecutionPolicy executionPolicy,
            @Min(value = MIN_USER_COOLDOWN_SECONDS, message = USER_COOLDOWN_RANGE_MESSAGE)
            @Max(value = MAX_USER_COOLDOWN_SECONDS, message = USER_COOLDOWN_RANGE_MESSAGE)
            Integer userCooldownSeconds
    ) {
        @AssertTrue(message = "userCooldownSeconds is required for interval policy")
        public boolean hasRequiredCooldown() {
            return executionPolicy != CommandExecutionPolicy.USER_INTERVAL || userCooldownSeconds != null;
        }
    }

    record ExecutionRecord(
            @NotNull(message = "executedAt is required") Instant executedAt
    ) {
    }

    record ObserveUserCommand(
            @NotBlank(message = "userId is required") String userId,
            String displayName
    ) {
    }

    record ExecutionData(
            @Positive(message = "commandId must be positive") long commandId,
            @NotBlank(message = "userId is required") String userId,
            @NotNull(message = "executedAt is required") Instant executedAt,
            @NotNull(message = "executionPolicy is required") CommandExecutionPolicy executionPolicy,
            @Min(value = MIN_USER_COOLDOWN_SECONDS, message = USER_COOLDOWN_RANGE_MESSAGE)
            @Max(value = MAX_USER_COOLDOWN_SECONDS, message = USER_COOLDOWN_RANGE_MESSAGE)
            Integer cooldownSeconds,
            Instant calendarDayStartedAt
    ) {
        @AssertTrue(message = "cooldownSeconds is required for interval policy")
        public boolean hasRequiredCooldown() {
            return executionPolicy != CommandExecutionPolicy.USER_INTERVAL || cooldownSeconds != null;
        }

        @AssertTrue(message = "calendarDayStartedAt is required for calendar-day policy")
        public boolean hasRequiredCalendarDayStart() {
            return executionPolicy != CommandExecutionPolicy.USER_CALENDAR_DAY || calendarDayStartedAt != null;
        }
    }
}
