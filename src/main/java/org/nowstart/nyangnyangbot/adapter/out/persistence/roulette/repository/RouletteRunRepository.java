package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RouletteRunRepository extends JpaRepository<RouletteRun, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select run from RouletteRun run where run.donationId = :id")
    Optional<RouletteRun> findByIdForUpdate(@Param("id") Long id);

    Page<RouletteRun> findAllByOrderByCreatedAtDescDonationIdDesc(Pageable pageable);

    @Query(value = """
            SELECT run.donation_id
              FROM roulette_run run
             WHERE run.status = 'READY'
               AND (
                    EXISTS (
                        SELECT 1
                          FROM roulette_round round_result
                         WHERE round_result.roulette_run_id = run.donation_id
                           AND round_result.status = 'CONFIRMED'
                    )
                    OR NOT EXISTS (
                        SELECT 1
                          FROM overlay_display_job display_job
                         WHERE display_job.roulette_run_id = run.donation_id
                    )
               )
             ORDER BY CASE WHEN run.donation_id > :afterRunId THEN 0 ELSE 1 END,
                      run.donation_id
             LIMIT :limit
            """, nativeQuery = true)
    List<Long> findRunIdsNeedingRecovery(
            @Param("afterRunId") long afterRunId,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT MAX(run.donation_id)
              FROM roulette_run run
             WHERE run.status = 'READY'
               AND (
                    EXISTS (
                        SELECT 1
                          FROM roulette_round round_result
                         WHERE round_result.roulette_run_id = run.donation_id
                           AND round_result.status = 'CONFIRMED'
                    )
                    OR NOT EXISTS (
                        SELECT 1
                          FROM overlay_display_job display_job
                         WHERE display_job.roulette_run_id = run.donation_id
                    )
               )
            """, nativeQuery = true)
    Long findMaxRunIdNeedingRecovery();
}
