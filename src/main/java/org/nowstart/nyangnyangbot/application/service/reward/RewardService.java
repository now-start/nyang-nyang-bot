package org.nowstart.nyangnyangbot.application.service.reward;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase.AdjustPointCommand;
import org.nowstart.nyangnyangbot.application.port.in.point.GrantPointUseCase;
import org.nowstart.nyangnyangbot.application.port.in.reward.GrantRewardUseCase;
import org.nowstart.nyangnyangbot.application.port.in.reward.QueryRewardUseCase;
import org.nowstart.nyangnyangbot.application.port.out.reward.RewardPort;
import org.nowstart.nyangnyangbot.application.port.out.reward.RewardPort.CreateRewardCommand;
import org.nowstart.nyangnyangbot.application.port.out.reward.RewardPort.RewardRecord;
import org.nowstart.nyangnyangbot.domain.point.PointSourceType;
import org.nowstart.nyangnyangbot.domain.reward.RewardPolicy;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardGrantStatus;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class RewardService implements GrantRewardUseCase, QueryRewardUseCase {

    private static final int MAX_REWARD_QUERY_LIMIT = 100;
    private final RewardPolicy rewardPolicy = new RewardPolicy();
    private final RewardPort rewardPort;
    private final GrantPointUseCase grantPointUseCase;

    @Override
    @Transactional
    public QueryRewardUseCase.RewardResult grantManual(
            GrantManualRewardCommand command,
            String actorUserId
    ) {
        if (actorUserId == null || actorUserId.isBlank()) {
            throw new IllegalArgumentException("actorUserId is required");
        }
        if (!rewardPort.lockUser(command.userId())) {
            throw new IllegalArgumentException("reward user not found");
        }
        String idempotencyKey = manualIdempotencyKey(command.idempotencyKey());
        RewardRecord existing = rewardPort.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            validateManualReplay(existing, command, actorUserId);
            return rewardResult(existing);
        }
        RewardType rewardType = RewardType.valueOf(command.rewardType().trim());
        ConversionMode conversionMode = ConversionMode.valueOf(command.conversionMode().trim());
        requireAutoPointDelta(conversionMode, command.pointDelta());
        Long ledgerEntryId = grantPoints(
                command.userId(),
                null,
                command.pointDelta(),
                PointSourceType.REWARD_MANUAL,
                null,
                command.description(),
                command.privateNote(),
                actorUserId,
                idempotencyKey,
                conversionMode,
                false
        );
        RewardGrantStatus status = rewardPolicy.initialStatus(conversionMode);
        rewardPolicy.validateGrant(
                command.userId(),
                null,
                ledgerEntryId,
                command.label(),
                rewardType,
                conversionMode,
                command.pointDelta(),
                status,
                command.description(),
                actorUserId,
                idempotencyKey
        );
        return rewardResult(rewardPort.createGrant(new CreateRewardCommand(
                command.userId(),
                null,
                ledgerEntryId,
                command.label().trim(),
                rewardType,
                conversionMode,
                command.pointDelta(),
                status,
                command.description().trim(),
                trimToNull(command.privateNote()),
                actorUserId,
                idempotencyKey,
                now()
        )));
    }

    @Transactional
    public RewardRecord grantRoulette(RouletteRewardCommand command) {
        RewardRecord existing = rewardPort.findByRouletteRoundId(command.roundId()).orElse(null);
        if (existing != null) {
            return existing;
        }
        String idempotencyKey = "roulette-round:" + command.roundId();
        requireAutoPointDelta(command.conversionMode(), command.pointDelta());
        Long ledgerEntryId = grantPoints(
                command.userId(),
                command.donorDisplayName(),
                command.pointDelta(),
                PointSourceType.REWARD_ROULETTE,
                command.ingestionKey(),
                command.description(),
                command.privateNote(),
                null,
                idempotencyKey,
                command.conversionMode(),
                false
        );
        RewardGrantStatus status = rewardPolicy.initialStatus(command.conversionMode());
        rewardPolicy.validateGrant(
                command.userId(),
                command.roundId(),
                ledgerEntryId,
                command.label(),
                command.rewardType(),
                command.conversionMode(),
                command.pointDelta(),
                status,
                command.description(),
                null,
                idempotencyKey
        );
        return rewardPort.createGrant(new CreateRewardCommand(
                command.userId(),
                command.roundId(),
                ledgerEntryId,
                command.label(),
                command.rewardType(),
                command.conversionMode(),
                command.pointDelta(),
                status,
                command.description(),
                command.privateNote(),
                null,
                idempotencyKey,
                now()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<QueryRewardUseCase.RewardResult> getUserRewards(String userId, String status, int limit) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (limit < 1 || limit > MAX_REWARD_QUERY_LIMIT) {
            throw new IllegalArgumentException("reward query limit must be between 1 and " + MAX_REWARD_QUERY_LIMIT);
        }
        List<RewardRecord> rewards = status == null || status.isBlank()
                ? rewardPort.findByUserId(userId, limit)
                : rewardPort.findByUserIdAndStatus(userId, RewardGrantStatus.valueOf(status.trim()), limit);
        return rewards.stream().map(this::rewardResult).toList();
    }

    Instant now() {
        return Instant.now();
    }

    private Long grantPoints(
            String userId,
            String displayName,
            Long pointDelta,
            PointSourceType sourceType,
            String sourceReference,
            String description,
            String privateNote,
            String actorUserId,
            String idempotencyKey,
            ConversionMode conversionMode,
            boolean createIfMissing
    ) {
        if (conversionMode != ConversionMode.AUTO) {
            return null;
        }
        var result = grantPointUseCase.grant(AdjustPointCommand.builder()
                .userId(userId)
                .displayName(displayName)
                .delta(pointDelta == null ? 0 : pointDelta)
                .sourceType(sourceType)
                .sourceReference(sourceReference)
                .description(description)
                .privateNote(privateNote)
                .actorUserId(actorUserId)
                .idempotencyKey(idempotencyKey)
                .allowNegativeBalance(true)
                .createIfMissing(createIfMissing)
                .build());
        return result.ledgerId();
    }

    private QueryRewardUseCase.RewardResult rewardResult(RewardRecord reward) {
        return new QueryRewardUseCase.RewardResult(
                reward.id(),
                reward.userId(),
                reward.rouletteRoundId(),
                reward.pointLedgerEntryId(),
                reward.label(),
                reward.rewardType().name(),
                reward.conversionMode().name(),
                reward.pointDelta(),
                reward.status().name(),
                reward.description(),
                reward.idempotencyKey(),
                reward.createdAt()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void requireAutoPointDelta(ConversionMode conversionMode, Long pointDelta) {
        if (conversionMode == ConversionMode.AUTO && (pointDelta == null || pointDelta == 0)) {
            throw new IllegalArgumentException("AUTO reward requires non-zero pointDelta");
        }
    }

    private String manualIdempotencyKey(String value) {
        return "reward-manual:" + value.trim();
    }

    private void validateManualReplay(
            RewardRecord existing,
            GrantManualRewardCommand command,
            String actorUserId
    ) {
        if (!existing.userId().equals(command.userId())
                || existing.rouletteRoundId() != null
                || !existing.label().equals(command.label().trim())
                || existing.rewardType() != RewardType.valueOf(command.rewardType().trim())
                || existing.conversionMode() != ConversionMode.valueOf(command.conversionMode().trim())
                || !java.util.Objects.equals(existing.pointDelta(), command.pointDelta())
                || !existing.description().equals(command.description().trim())
                || !java.util.Objects.equals(existing.privateNote(), trimToNull(command.privateNote()))
                || !java.util.Objects.equals(existing.actorUserId(), actorUserId)) {
            throw new IllegalArgumentException("reward idempotency key conflicts with existing grant");
        }
    }

    public record RouletteRewardCommand(
            Long roundId,
            String userId,
            String donorDisplayName,
            String ingestionKey,
            String label,
            RewardType rewardType,
            ConversionMode conversionMode,
            Long pointDelta,
            String description,
            String privateNote
    ) {
    }
}
