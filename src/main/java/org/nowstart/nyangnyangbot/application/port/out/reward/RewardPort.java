package org.nowstart.nyangnyangbot.application.port.out.reward;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundResult;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardGrantStatus;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

public interface RewardPort {

    void createGrant(CreateRewardCommand command);

    boolean existsByRouletteRoundId(Long rouletteRoundId);

    List<RewardRecord> findByUserId(String userId, int limit);

    List<RewardRecord> findByUserIdAndStatus(String userId, RewardGrantStatus status, int limit);

    record CreateRewardCommand(
            @NotBlank(message = "userId is required") String userId,
            @NotNull(message = "rouletteRoundId is required")
            @Positive(message = "rouletteRoundId must be positive") Long rouletteRoundId,
            Long pointLedgerEntryId,
            @NotBlank(message = "label is required") String label,
            @NotNull(message = "rewardType is required") RewardType rewardType,
            @NotNull(message = "conversionMode is required") ConversionMode conversionMode,
            Long pointDelta,
            @NotNull(message = "status is required") RewardGrantStatus status,
            String description,
            String privateNote,
            String actorUserId,
            @NotBlank(message = "idempotencyKey is required") String idempotencyKey,
            @NotNull(message = "createdAt is required") Instant createdAt
    ) {
    }

    record RewardRecord(
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive") Long id,
            Long pointLedgerEntryId,
            @NotBlank(message = "label is required") String label,
            @NotNull(message = "rewardType is required") RewardType rewardType,
            @NotNull(message = "conversionMode is required") ConversionMode conversionMode,
            Long pointDelta,
            @NotNull(message = "status is required") RewardGrantStatus status,
            String description,
            @NotNull(message = "createdAt is required") Instant createdAt
    ) {
    }
}
