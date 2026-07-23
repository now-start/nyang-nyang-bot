package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteRound;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RouletteRoundRepository extends JpaRepository<RouletteRound, Long> {

    List<RouletteRound> findByRouletteRun_DonationIdOrderByRoundNoAsc(Long rouletteRunId);

    @Query(value = """
            SELECT round_result.roulette_run_id AS runId,
                   COUNT(*) AS roundCount,
                   SUM(CASE WHEN round_result.status = 'APPLIED' THEN 1 ELSE 0 END) AS appliedCount,
                   SUM(CASE WHEN round_result.status = 'FAILED' THEN 1 ELSE 0 END) AS failedCount
              FROM roulette_round round_result
             WHERE round_result.roulette_run_id IN (:runIds)
             GROUP BY round_result.roulette_run_id
            """, nativeQuery = true)
    List<RunRoundSummaryProjection> summarizeRuns(@Param("runIds") List<Long> runIds);

    long countByRouletteRun_DonationId(Long rouletteRunId);

    @Query("""
            select round.id as id,
                   round.roundNo as roundNo,
                   option.label as optionLabel,
                   option.losing as losing,
                   option.rewardType as rewardType,
                   option.conversionMode as conversionMode,
                   option.pointDelta as pointDelta,
                   round.status as status,
                   round.failureReason as failureReason
              from RouletteRound round
              join round.rouletteOption option
              join round.rouletteRun run
             where run.donationId = :runId
             order by round.roundNo asc
            """)
    List<DisplayRoundProjection> findDisplayRoundsByRunId(
            @Param("runId") Long runId,
            Pageable pageable
    );

    @Query("""
            select round.roundNo as roundNo,
                   option.label as itemLabel
              from RouletteRound round
              join round.rouletteOption option
              join round.rouletteRun run
              join run.donation donation
              join donation.donorUserAccount donor
             where donor.userId = :userId
             order by round.createdAt desc, round.id desc
            """)
    List<RecentRoundProjection> findRecentByUserId(@Param("userId") String userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select round from RouletteRound round where round.id = :id")
    Optional<RouletteRound> findByIdForUpdate(@Param("id") Long id);

    interface RecentRoundProjection {
        Integer getRoundNo();

        String getItemLabel();
    }

    interface RunRoundSummaryProjection {
        Long getRunId();

        long getRoundCount();

        long getAppliedCount();

        long getFailedCount();
    }

    interface DisplayRoundProjection {
        Long getId();

        Integer getRoundNo();

        String getOptionLabel();

        boolean getLosing();

        org.nowstart.nyangnyangbot.domain.type.RewardType getRewardType();

        org.nowstart.nyangnyangbot.domain.type.ConversionMode getConversionMode();

        Long getPointDelta();

        org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus getStatus();

        String getFailureReason();
    }
}
