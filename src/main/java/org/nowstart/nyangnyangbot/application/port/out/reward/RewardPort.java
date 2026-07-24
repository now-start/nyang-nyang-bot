package org.nowstart.nyangnyangbot.application.port.out.reward;

import java.time.Instant;
import java.util.List;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardGrantStatus;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

public interface RewardPort {

    void createGrant(CreateRewardCommand command);

    boolean existsByRouletteRoundId(Long rouletteRoundId);

    List<RewardRecord> findByUserId(String userId, int limit);

    List<RewardRecord> findByUserIdAndStatus(String userId, RewardGrantStatus status, int limit);

    record CreateRewardCommand(
            String userId,
            Long rouletteRoundId,
            Long pointLedgerEntryId,
            String label,
            RewardType rewardType,
            ConversionMode conversionMode,
            Long pointDelta,
            RewardGrantStatus status,
            String description,
            String privateNote,
            String actorUserId,
            String idempotencyKey,
            Instant createdAt
    ) {
    }

    record RewardRecord(
            Long id,
            Long pointLedgerEntryId,
            String label,
            RewardType rewardType,
            ConversionMode conversionMode,
            Long pointDelta,
            RewardGrantStatus status,
            String description,
            Instant createdAt
    ) {
    }
}
