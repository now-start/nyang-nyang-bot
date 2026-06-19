package org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.repository;

import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.entity.OverlayDisplayEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.type.OverlayDisplayStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OverlayDisplayEventRepository extends JpaRepository<OverlayDisplayEvent, Long> {

    List<OverlayDisplayEvent> findByStatusAndExpiresAtBefore(
            OverlayDisplayStatus status,
            LocalDateTime expiresAt
    );

    Optional<OverlayDisplayEvent> findFirstByStatusAndExpiresAtAfterOrderByCreateDateAsc(
            OverlayDisplayStatus status,
            LocalDateTime expiresAt
    );

    List<OverlayDisplayEvent> findByRouletteEventIdOrderByCreateDateDesc(Long rouletteEventId);
}
