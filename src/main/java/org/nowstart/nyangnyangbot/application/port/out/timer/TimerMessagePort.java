package org.nowstart.nyangnyangbot.application.port.out.timer;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundResult;

public interface TimerMessagePort {

    List<TimerMessageRecord> findAllOrderByIdDesc();

    Optional<TimerMessageRecord> findByIdForUpdate(Long timerMessageId);

    TimerMessageRecord create(CreateData data);

    TimerMessageRecord update(UpdateData data);

    void incrementActiveChatCounts();

    List<Long> findClaimCandidateIds(LocalDateTime now, int limit);

    Optional<ClaimedTimerMessage> claimDue(
            Long timerMessageId,
            String claimToken,
            LocalDateTime now,
            LocalDateTime claimExpiresAt
    );

    boolean completeClaim(
            Long timerMessageId,
            String claimToken,
            LocalDateTime claimedNextRunAt,
            Integer claimedIntervalMinutes,
            LocalDateTime sentAt,
            LocalDateTime nextRunAt
    );

    boolean releaseClaim(
            Long timerMessageId,
            String claimToken,
            LocalDateTime claimedNextRunAt,
            Integer claimedIntervalMinutes,
            LocalDateTime retryAt
    );

    record TimerMessageRecord(
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive")
            Long id,
            @NotBlank(message = "messageTemplate is required")
            String messageTemplate,
            @NotNull(message = "intervalMinutes is required")
            @Min(value = 5, message = "intervalMinutes must be between 5 and 1440")
            @Max(value = 1440, message = "intervalMinutes must be between 5 and 1440")
            Integer intervalMinutes,
            @NotNull(message = "minChatCount is required")
            @Min(value = 1, message = "minChatCount must be between 1 and 10000")
            @Max(value = 10000, message = "minChatCount must be between 1 and 10000")
            Integer minChatCount,
            boolean active,
            @Min(value = 0, message = "chatCountSinceLastSend must not be negative")
            long chatCountSinceLastSend,
            LocalDateTime lastSentAt,
            LocalDateTime nextRunAt,
            String createdBy,
            String updatedBy,
            LocalDateTime createDate,
            LocalDateTime modifyDate
    ) {
    }

    record CreateData(
            @NotBlank(message = "messageTemplate is required") String messageTemplate,
            @NotNull(message = "intervalMinutes is required") Integer intervalMinutes,
            @NotNull(message = "minChatCount is required") Integer minChatCount,
            boolean active,
            LocalDateTime nextRunAt,
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
            LocalDateTime nextRunAt,
            boolean resetSchedule,
            String updatedBy
    ) {
    }

    record ClaimedTimerMessage(
            @NotNull(message = "id is required") @Positive(message = "id must be positive") Long id,
            @NotBlank(message = "messageTemplate is required") String messageTemplate,
            @NotNull(message = "intervalMinutes is required") Integer intervalMinutes,
            @NotNull(message = "claimedNextRunAt is required") LocalDateTime claimedNextRunAt,
            @NotBlank(message = "claimToken is required") String claimToken
    ) {
    }
}
