package org.nowstart.nyangnyangbot.application.port.out.timer;

import static org.nowstart.nyangnyangbot.domain.timer.TimerMessagePolicy.CHAT_COUNT_RANGE_MESSAGE;
import static org.nowstart.nyangnyangbot.domain.timer.TimerMessagePolicy.INTERVAL_RANGE_MESSAGE;
import static org.nowstart.nyangnyangbot.domain.timer.TimerMessagePolicy.MAX_CHAT_COUNT;
import static org.nowstart.nyangnyangbot.domain.timer.TimerMessagePolicy.MAX_INTERVAL_MINUTES;
import static org.nowstart.nyangnyangbot.domain.timer.TimerMessagePolicy.MAX_TEMPLATE_LENGTH;
import static org.nowstart.nyangnyangbot.domain.timer.TimerMessagePolicy.MIN_CHAT_COUNT;
import static org.nowstart.nyangnyangbot.domain.timer.TimerMessagePolicy.MIN_INTERVAL_MINUTES;
import static org.nowstart.nyangnyangbot.domain.timer.TimerMessagePolicy.TEMPLATE_LENGTH_MESSAGE;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TimerMessagePort {

    /** 모든 타이머 메시지를 식별자 내림차순으로 반환한다. */
    @Valid
    List<TimerMessageRecord> findAllOrderByIdDesc();

    /** 타이머 메시지를 조회하고 현재 트랜잭션 동안 쓰기 잠금을 유지한다. */
    @Valid
    Optional<TimerMessageRecord> findByIdForUpdate(Long timerMessageId);

    /** 새 타이머 메시지를 저장하고 저장된 결과를 반환한다. */
    @Valid
    TimerMessageRecord create(@Valid @NotNull(message = "timer message data is required") CreateData data);

    /** 쓰기 잠금이 적용된 기존 타이머 메시지를 수정한다. */
    @Valid
    TimerMessageRecord update(@Valid @NotNull(message = "timer message data is required") UpdateData data);

    /** 모든 활성 타이머 메시지의 채팅 횟수를 원자적으로 증가시킨다. */
    void incrementActiveChatCounts();

    /** 실행 시각이 되었고 선점 가능한 후보 식별자를 최대 {@code limit}개 반환한다. */
    List<Long> findClaimCandidateIds(Instant now, int limit);

    /** 실행할 메시지를 원자적으로 선점하며, 더 이상 선점할 수 없으면 빈 값을 반환한다. */
    @Valid
    Optional<ClaimedTimerMessage> claimDue(
            Long timerMessageId,
            String claimToken,
            Instant now,
            Instant claimExpiresAt
    );

    /**
     * 토큰과 선점 당시 일정 정보가 여전히 일치하는 경우에만 선점을 완료 처리한다.
     *
     * @return 하나의 선점을 완료했으면 {@code true}, 그렇지 않으면 {@code false}
     */
    boolean completeClaim(
            Long timerMessageId,
            String claimToken,
            Instant claimedNextRunAt,
            Integer claimedIntervalMinutes,
            Instant sentAt,
            Instant nextRunAt
    );

    /**
     * 토큰과 선점 당시 일정 정보가 여전히 일치하는 경우에만 재시도를 위해 선점을 해제한다.
     *
     * @return 하나의 선점을 해제했으면 {@code true}, 그렇지 않으면 {@code false}
     */
    boolean releaseClaim(
            Long timerMessageId,
            String claimToken,
            Instant claimedNextRunAt,
            Integer claimedIntervalMinutes,
            Instant retryAt
    );

    record TimerMessageRecord(
            @NotNull(message = "id is required")
            @Positive(message = "id must be positive")
            Long id,
            @NotBlank(message = "messageTemplate is required")
            @Size(max = MAX_TEMPLATE_LENGTH, message = TEMPLATE_LENGTH_MESSAGE)
            String messageTemplate,
            @NotNull(message = "intervalMinutes is required")
            @Min(value = MIN_INTERVAL_MINUTES, message = INTERVAL_RANGE_MESSAGE)
            @Max(value = MAX_INTERVAL_MINUTES, message = INTERVAL_RANGE_MESSAGE)
            Integer intervalMinutes,
            @NotNull(message = "minChatCount is required")
            @Min(value = MIN_CHAT_COUNT, message = CHAT_COUNT_RANGE_MESSAGE)
            @Max(value = MAX_CHAT_COUNT, message = CHAT_COUNT_RANGE_MESSAGE)
            Integer minChatCount,
            boolean active,
            @Min(value = 0, message = "chatCountSinceLastSend must not be negative")
            long chatCountSinceLastSend,
            Instant lastSentAt,
            Instant nextRunAt,
            String createdBy,
            String updatedBy
    ) {
    }

    record CreateData(
            @NotBlank(message = "messageTemplate is required")
            @Size(max = MAX_TEMPLATE_LENGTH, message = TEMPLATE_LENGTH_MESSAGE) String messageTemplate,
            @NotNull(message = "intervalMinutes is required")
            @Min(value = MIN_INTERVAL_MINUTES, message = INTERVAL_RANGE_MESSAGE)
            @Max(value = MAX_INTERVAL_MINUTES, message = INTERVAL_RANGE_MESSAGE) Integer intervalMinutes,
            @NotNull(message = "minChatCount is required")
            @Min(value = MIN_CHAT_COUNT, message = CHAT_COUNT_RANGE_MESSAGE)
            @Max(value = MAX_CHAT_COUNT, message = CHAT_COUNT_RANGE_MESSAGE) Integer minChatCount,
            boolean active,
            Instant nextRunAt,
            String createdBy,
            String updatedBy
    ) {
    }

    record UpdateData(
            @NotNull(message = "id is required") @Positive(message = "id must be positive") Long id,
            @NotBlank(message = "messageTemplate is required")
            @Size(max = MAX_TEMPLATE_LENGTH, message = TEMPLATE_LENGTH_MESSAGE) String messageTemplate,
            @NotNull(message = "intervalMinutes is required")
            @Min(value = MIN_INTERVAL_MINUTES, message = INTERVAL_RANGE_MESSAGE)
            @Max(value = MAX_INTERVAL_MINUTES, message = INTERVAL_RANGE_MESSAGE) Integer intervalMinutes,
            @NotNull(message = "minChatCount is required")
            @Min(value = MIN_CHAT_COUNT, message = CHAT_COUNT_RANGE_MESSAGE)
            @Max(value = MAX_CHAT_COUNT, message = CHAT_COUNT_RANGE_MESSAGE) Integer minChatCount,
            boolean active,
            Instant nextRunAt,
            boolean resetSchedule,
            String updatedBy
    ) {
    }

    record ClaimedTimerMessage(
            @NotNull(message = "id is required") @Positive(message = "id must be positive") Long id,
            @NotBlank(message = "messageTemplate is required")
            @Size(max = MAX_TEMPLATE_LENGTH, message = TEMPLATE_LENGTH_MESSAGE) String messageTemplate,
            @NotNull(message = "intervalMinutes is required")
            @Min(value = MIN_INTERVAL_MINUTES, message = INTERVAL_RANGE_MESSAGE)
            @Max(value = MAX_INTERVAL_MINUTES, message = INTERVAL_RANGE_MESSAGE) Integer intervalMinutes,
            @NotNull(message = "claimedNextRunAt is required") Instant claimedNextRunAt,
            @NotBlank(message = "claimToken is required") String claimToken
    ) {
    }
}
