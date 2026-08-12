package org.nowstart.nyangnyangbot.application.port.out.command;

import static org.nowstart.nyangnyangbot.domain.command.CommandPolicy.TEMPLATE_LENGTH_MESSAGE;
import static org.nowstart.nyangnyangbot.domain.command.CommandPolicy.USER_COOLDOWN_RANGE_MESSAGE;
import static org.nowstart.nyangnyangbot.domain.command.CommandPolicy.MAX_TEMPLATE_LENGTH;
import static org.nowstart.nyangnyangbot.domain.command.CommandPolicy.MAX_USER_COOLDOWN_SECONDS;
import static org.nowstart.nyangnyangbot.domain.command.CommandPolicy.MIN_USER_COOLDOWN_SECONDS;

import jakarta.validation.Valid;
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

    /** 정규화된 트리거로 활성 명령어를 조회하고 현재 트랜잭션 동안 쓰기 잠금을 유지한다. */
    Optional<@Valid LockedCommand> lockActiveCommand(String normalizedTrigger);

    /** 사용자를 생성하거나 갱신하고 현재 트랜잭션 동안 해당 사용자의 쓰기 잠금을 유지한다. */
    void observeAndLockUser(
            @Valid @NotNull(message = "observe user command is required") ObserveUserCommand command
    );

    /** 데이터베이스 서버의 현재 시각을 반환한다. */
    @NotNull(message = "database time is required")
    Instant currentDatabaseTime();

    /** 사용자의 최근 실행 기록을 조회하고 현재 트랜잭션 동안 쓰기 잠금을 유지한다. */
    Optional<@Valid ExecutionRecord> findLatestForUpdate(long commandId, String userId);

    /** 사용자가 지정한 달력 날짜 구간에 해당 명령어를 이미 실행했는지 반환한다. */
    boolean existsCalendarDayStartedAt(long commandId, String userId, Instant calendarDayStartedAt);

    /** 변경할 수 없는 명령어 실행 기록을 추가한다. */
    void append(@Valid @NotNull(message = "execution data is required") ExecutionData data);

    /** 해당 명령어의 전체 실행 횟수를 반환한다. */
    long countAll(long commandId);

    /** 해당 사용자의 명령어 실행 횟수를 반환한다. */
    long countForUser(long commandId, String userId);

    /** 사용자의 명령어 실행 기록에 저장된 달력 날짜 구간 시작 시각을 반환한다. */
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
