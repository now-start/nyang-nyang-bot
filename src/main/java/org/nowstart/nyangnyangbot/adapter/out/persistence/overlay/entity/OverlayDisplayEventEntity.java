package org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.entity;

import org.nowstart.nyangnyangbot.adapter.out.persistence.common.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteEventEntity;
import org.nowstart.nyangnyangbot.domain.type.OverlayDisplayStatus;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OverlayDisplayEventEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private RouletteEventEntity rouletteEvent;
    private Long replayOfDisplayEventId;
    @Enumerated(EnumType.STRING)
    private OverlayDisplayStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime fetchedAt;
    private LocalDateTime displayedAt;

    public void markDisplaying(LocalDateTime fetchedAt) {
        status = OverlayDisplayStatus.DISPLAYING;
        this.fetchedAt = fetchedAt;
    }

    public void markDisplayed(LocalDateTime displayedAt) {
        status = OverlayDisplayStatus.DISPLAYED;
        this.displayedAt = displayedAt;
    }

    public void markMissed() {
        status = OverlayDisplayStatus.MISSED;
    }
}
