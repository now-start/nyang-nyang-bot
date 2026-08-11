package org.nowstart.nyangnyangbot.application.port.out.weekly;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.List;

public interface WeeklyChatCountPort {

    void increment(IncrementWeeklyChatCommand command);

    List<WeeklyChatRankRecord> findWeeklyRanks(Instant weekStartedAt, int limit);

    record IncrementWeeklyChatCommand(
            @NotNull(message = "weekStartedAt is required") Instant weekStartedAt,
            @NotBlank(message = "userId is required") String userId
    ) {
    }

    record WeeklyChatRankRecord(
            @NotNull(message = "rank is required")
            @Positive(message = "rank must be positive") Integer rank,
            String displayName,
            @NotNull(message = "chatCount is required")
            @PositiveOrZero(message = "chatCount must not be negative") Long chatCount
    ) {
    }
}
