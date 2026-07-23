package org.nowstart.nyangnyangbot.adapter.out.persistence.reward.repository;

import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.adapter.out.persistence.reward.entity.RewardGrant;
import org.nowstart.nyangnyangbot.domain.type.RewardGrantStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardGrantRepository extends JpaRepository<RewardGrant, Long> {

    Optional<RewardGrant> findByIdempotencyKey(String idempotencyKey);

    Optional<RewardGrant> findByRouletteRound_Id(Long rouletteRoundId);

    List<RewardGrant> findByUserAccount_UserIdOrderByCreatedAtDescIdDesc(String userId, Pageable pageable);

    List<RewardGrant> findByUserAccount_UserIdAndStatusOrderByCreatedAtDescIdDesc(
            String userId,
            RewardGrantStatus status,
            Pageable pageable
    );
}
