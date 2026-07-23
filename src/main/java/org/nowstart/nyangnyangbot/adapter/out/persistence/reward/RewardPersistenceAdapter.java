package org.nowstart.nyangnyangbot.adapter.out.persistence.reward;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.entity.PointLedgerEntry;
import org.nowstart.nyangnyangbot.adapter.out.persistence.reward.entity.RewardGrant;
import org.nowstart.nyangnyangbot.adapter.out.persistence.reward.repository.RewardGrantRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteRound;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.application.port.out.reward.RewardPort;
import org.nowstart.nyangnyangbot.domain.type.RewardGrantStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RewardPersistenceAdapter implements RewardPort {

    private final RewardGrantRepository rewardGrantRepository;
    private final EntityManager entityManager;
    private final OutboundContractValidator contractValidator;

    @Override
    @Transactional
    public RewardRecord createGrant(CreateRewardCommand command) {
        RewardGrant grant = RewardGrant.builder()
                .userAccount(reference(UserAccount.class, command.userId()))
                .rouletteRound(reference(RouletteRound.class, command.rouletteRoundId()))
                .pointLedgerEntry(reference(PointLedgerEntry.class, command.pointLedgerEntryId()))
                .label(command.label())
                .rewardType(command.rewardType())
                .conversionMode(command.conversionMode())
                .pointDelta(command.pointDelta())
                .status(command.status())
                .description(command.description())
                .privateNote(command.privateNote())
                .actorUserAccount(reference(UserAccount.class, command.actorUserId()))
                .idempotencyKey(command.idempotencyKey())
                .createdAt(command.createdAt())
                .updatedAt(command.createdAt())
                .build();
        return rewardRecord(rewardGrantRepository.save(grant));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RewardRecord> findByRouletteRoundId(Long rouletteRoundId) {
        return rewardGrantRepository.findByRouletteRound_Id(rouletteRoundId).map(this::rewardRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RewardRecord> findByUserId(String userId, int limit) {
        return rewardGrantRepository.findByUserAccount_UserIdOrderByCreatedAtDescIdDesc(
                        userId,
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(this::rewardRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RewardRecord> findByUserIdAndStatus(String userId, RewardGrantStatus status, int limit) {
        return rewardGrantRepository.findByUserAccount_UserIdAndStatusOrderByCreatedAtDescIdDesc(
                        userId,
                        status,
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(this::rewardRecord)
                .toList();
    }

    private RewardRecord rewardRecord(RewardGrant grant) {
        return contractValidator.persistenceResult("reward.grant", new RewardRecord(
                grant.getId(),
                grant.getUserAccount().getUserId(),
                grant.getRouletteRound() == null ? null : grant.getRouletteRound().getId(),
                grant.getPointLedgerEntry() == null ? null : grant.getPointLedgerEntry().getId(),
                grant.getLabel(),
                grant.getRewardType(),
                grant.getConversionMode(),
                grant.getPointDelta(),
                grant.getStatus(),
                grant.getDescription(),
                grant.getPrivateNote(),
                grant.getActorUserAccount() == null ? null : grant.getActorUserAccount().getUserId(),
                grant.getIdempotencyKey(),
                grant.getCreatedAt(),
                grant.getUpdatedAt()
        ));
    }

    private <T> T reference(Class<T> type, Object id) {
        return id == null ? null : entityManager.getReference(type, id);
    }
}
