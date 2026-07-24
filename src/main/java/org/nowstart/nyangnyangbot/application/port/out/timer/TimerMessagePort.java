package org.nowstart.nyangnyangbot.application.port.out.timer;

import static org.nowstart.nyangnyangbot.domain.timer.TimerMessagePolicy.CHAT_COUNT_RANGE_MESSAGE;
import static org.nowstart.nyangnyangbot.domain.timer.TimerMessagePolicy.INTERVAL_RANGE_MESSAGE;
import static org.nowstart.nyangnyangbot.domain.timer.TimerMessagePolicy.MAX_CHAT_COUNT;
import static org.nowstart.nyangnyangbot.domain.timer.TimerMessagePolicy.MAX_INTERVAL_MINUTES;
import static org.nowstart.nyangnyangbot.domain.timer.TimerMessagePolicy.MIN_CHAT_COUNT;
import static org.nowstart.nyangnyangbot.domain.timer.TimerMessagePolicy.MIN_INTERVAL_MINUTES;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundResult;

public interface TimerMessagePort {

    List<TimerMessageRecord> findAllOrderByIdDesc();

    Optional<TimerMessageRecord> findByIdForUpdate(Long timerMessageId);

    TimerMessageRecord create(CreateData data);

    TimerMessageRecord update(UpdateData data);

    void incrementActiveChatCounts();

    List<Long> findClaimCandidateIds(Instant now, int limit);

    Optional<ClaimedTimerMessage> claimDue(
            Long timerMessageId,
            String claimToken,
            Instant now,
            Instant claimExpiresAt
    );

    boolean completeClaim(
            Long timerMessageId,
            String claimToken,
            Instant claimedNextRunAt,
            Integer claimedIntervalMinutes,
            Instant sentAt,
            Instant nextRunAt
    );

    boolean releaseClaim(
            Long timerMessageId,
            String claimToken,
            Instant claimedNextRunAt,
            Integer claimedIntervalMinutes,
            Instant retryAt
    );

    record TimerMessageRecord(
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive")
            Long id,
            @NotBlank(message = "messageTemplate is required")
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
            @NotBlank(message = "messageTemplate is required") String messageTemplate,
            @NotNull(message = "intervalMinutes is required") Integer intervalMinutes,
            @NotNull(message = "minChatCount is required") Integer minChatCount,
            boolean active,
            Instant nextRunAt,
            String createdBy,
            String updatedBy
    ) {
    }

    record UpdateData(
            @NotNull(message = "id is required") @Positive(message = "id must be positive") Long id,
            @NotBlank(message = "messageTemplate is required") String messageTemplate,
            @NotNull(message = "intervalMinutes is required") Integer intervalMinutes,
            @NotNull(message = "minChatCount is required") Integer minChatCount,
            boolean active,
            Instant nextRunAt,
            boolean resetSchedule,
            String updatedBy
    ) {
    }

    record ClaimedTimerMessage(
            @NotNull(message = "id is required") @Positive(message = "id must be positive") Long id,
            @NotBlank(message = "messageTemplate is required") String messageTemplate,
            @NotNull(message = "intervalMinutes is required") Integer intervalMinutes,
            @NotNull(message = "claimedNextRunAt is required") Instant claimedNextRunAt,
            @NotBlank(message = "claimToken is required") String claimToken
    ) {
    }
}
