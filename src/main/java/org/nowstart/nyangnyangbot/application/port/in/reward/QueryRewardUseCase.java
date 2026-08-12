package org.nowstart.nyangnyangbot.application.port.in.reward;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

public interface QueryRewardUseCase {

    int MIN_QUERY_LIMIT = 1;
    int MAX_QUERY_LIMIT = 100;

    /** 사용자의 보상을 최대 {@code limit}개 반환하며, 상태가 주어지면 해당 상태로 필터링한다. */
    List<RewardResult> getUserRewards(
            @NotBlank(message = "userId is required") String userId,
            String status,
            @Min(value = MIN_QUERY_LIMIT, message = "reward query limit must be at least 1")
            @Max(value = MAX_QUERY_LIMIT, message = "reward query limit must be 100 or less") int limit
    );

    record RewardResult(
            Long id,
            Long pointLedgerEntryId,
            String label,
            String rewardType,
            String conversionMode,
            Long pointDelta,
            String status,
            String description,
            Instant createdAt
    ) {
    }
}
