package org.nowstart.nyangnyangbot.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.nowstart.nyangnyangbot.data.type.OverlayDisplayStatus;

@Entity
@Table(name = "overlay_display_event")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OverlayDisplayEventEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "roulette_event_id")
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
