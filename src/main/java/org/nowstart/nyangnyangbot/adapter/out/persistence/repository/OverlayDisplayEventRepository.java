package org.nowstart.nyangnyangbot.adapter.out.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.adapter.out.persistence.entity.OverlayDisplayEventEntity;
import org.nowstart.nyangnyangbot.domain.type.OverlayDisplayStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OverlayDisplayEventRepository extends JpaRepository<OverlayDisplayEventEntity, Long> {

    List<OverlayDisplayEventEntity> findByStatusAndExpiresAtBefore(
            OverlayDisplayStatus status,
            LocalDateTime expiresAt
    );

    Optional<OverlayDisplayEventEntity> findFirstByStatusAndExpiresAtAfterOrderByCreateDateAsc(
            OverlayDisplayStatus status,
            LocalDateTime expiresAt
    );

    List<OverlayDisplayEventEntity> findByRouletteEventIdOrderByCreateDateDesc(Long rouletteEventId);
}
