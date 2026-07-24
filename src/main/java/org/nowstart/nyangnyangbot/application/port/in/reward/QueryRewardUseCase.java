package org.nowstart.nyangnyangbot.application.port.in.reward;

import java.time.Instant;
import java.util.List;

public interface QueryRewardUseCase {

    int MIN_QUERY_LIMIT = 1;
    int MAX_QUERY_LIMIT = 100;

    List<RewardResult> getUserRewards(String userId, String status, int limit);

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
