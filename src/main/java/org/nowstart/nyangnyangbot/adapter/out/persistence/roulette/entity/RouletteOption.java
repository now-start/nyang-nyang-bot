package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

@Entity
@Table(name = "roulette_option")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouletteOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "roulette_config_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private RouletteConfig rouletteConfig;

    @Column(nullable = false, length = 100, updatable = false)
    private String label;

    @Column(name = "probability_basis_points", nullable = false, updatable = false)
    private Integer probabilityBasisPoints;

    @Column(name = "is_losing", nullable = false, updatable = false)
    private boolean losing;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 32, updatable = false)
    private RewardType rewardType;

    @Enumerated(EnumType.STRING)
    @Column(name = "conversion_mode", nullable = false, length = 16, updatable = false)
    private ConversionMode conversionMode;

    @Column(name = "point_delta", updatable = false)
    private Long pointDelta;

    @Column(name = "display_order", nullable = false, updatable = false)
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
