package org.nowstart.nyangnyangbot.application.port.out.weekly;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.List;

public interface WeeklyChatCountPort {

    /** 데이터베이스 서버의 현재 시각을 반환한다. */
    @NotNull(message = "database time is required")
    Instant currentDatabaseTime();

    /** 지정한 주간 구간의 사용자 채팅 횟수를 원자적으로 증가시킨다. */
    void increment(
            @Valid @NotNull(message = "weekly chat command is required") IncrementWeeklyChatCommand command
    );

    /** 전달된 시각에 시작하는 주간 구간의 순위 사용자를 최대 {@code limit}명 반환한다. */
    List<@Valid WeeklyChatRankRecord> findWeeklyRanks(Instant weekStartedAt, int limit);

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
