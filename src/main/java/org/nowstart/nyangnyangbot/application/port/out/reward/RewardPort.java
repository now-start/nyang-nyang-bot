package org.nowstart.nyangnyangbot.application.port.out.reward;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardGrantStatus;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

public interface RewardPort {

    /** 룰렛 회차의 보상 지급 기록을 저장한다. */
    void createGrant(@Valid @NotNull(message = "reward command is required") CreateRewardCommand command);

    /** 해당 룰렛 회차의 보상 지급 기록이 이미 존재하는지 반환한다. */
    boolean existsByRouletteRoundId(Long rouletteRoundId);

    /** 사용자의 최근 보상을 최대 {@code limit}개 반환한다. */
    List<@Valid RewardRecord> findByUserId(String userId, int limit);

    /** 사용자의 최근 보상 중 요청한 상태의 항목을 최대 {@code limit}개 반환한다. */
    List<@Valid RewardRecord> findByUserIdAndStatus(String userId, RewardGrantStatus status, int limit);

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
            @NotNull(message = "id is required")
            @Positive(message = "id must be positive") Long id,
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
