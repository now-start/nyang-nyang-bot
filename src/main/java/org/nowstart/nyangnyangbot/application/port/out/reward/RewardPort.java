package org.nowstart.nyangnyangbot.application.port.out.reward;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardGrantStatus;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

public interface RewardPort {

    boolean lockUser(String userId);

    RewardRecord createGrant(CreateRewardCommand command);

    Optional<RewardRecord> findByIdempotencyKey(String idempotencyKey);

    Optional<RewardRecord> findByRouletteRoundId(Long rouletteRoundId);

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
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
