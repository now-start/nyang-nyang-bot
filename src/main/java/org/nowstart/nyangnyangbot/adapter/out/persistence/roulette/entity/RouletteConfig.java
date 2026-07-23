package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.nowstart.nyangnyangbot.domain.type.RouletteConfigStatus;

@Entity
@Table(name = "roulette_config")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouletteConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "trigger_token", nullable = false, length = 20)
    private String triggerToken;

    @Column(name = "price_per_round", nullable = false)
    private Long pricePerRound;

    @Column(name = "high_round_threshold", nullable = false)
    private Integer highRoundThreshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RouletteConfigStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void activate(Instant activatedAt) {
        requireStatus(RouletteConfigStatus.DRAFT);
        status = RouletteConfigStatus.ACTIVE;
        updatedAt = activatedAt;
    }

    public void archive(Instant archivedAt) {
        requireStatus(RouletteConfigStatus.ACTIVE);
        status = RouletteConfigStatus.ARCHIVED;
        updatedAt = archivedAt;
    }

    private void requireStatus(RouletteConfigStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("roulette config must be " + expected);
        }
    }
}
