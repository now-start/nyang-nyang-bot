package org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.entity.OverlayDisplayJob;
import org.nowstart.nyangnyangbot.domain.type.OverlayDisplayStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OverlayDisplayJobRepository extends JpaRepository<OverlayDisplayJob, Long> {

    Optional<OverlayDisplayJob> findFirstByRouletteRun_DonationIdOrderByCreatedAtDescIdDesc(Long rouletteRunId);

    Optional<OverlayDisplayJob> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from OverlayDisplayJob job where job.id = :id")
    Optional<OverlayDisplayJob> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select job
            from OverlayDisplayJob job
            where job.expiresAt > :now
              and (job.status = :pending
                or (job.status = :displaying and job.claimExpiresAt <= :now))
            order by job.createdAt, job.id
            """)
    List<OverlayDisplayJob> findClaimableForUpdate(
            @Param("now") Instant now,
            @Param("pending") OverlayDisplayStatus pending,
            @Param("displaying") OverlayDisplayStatus displaying,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update OverlayDisplayJob job
            set job.status = :missed,
                job.claimToken = null,
                job.claimExpiresAt = null,
                job.displayedAt = null,
                job.updatedAt = :now
            where job.expiresAt <= :now
              and job.status in :expirableStatuses
            """)
    int markExpiredMissed(
            @Param("now") Instant now,
            @Param("missed") OverlayDisplayStatus missed,
            @Param("expirableStatuses") List<OverlayDisplayStatus> expirableStatuses
    );
}
