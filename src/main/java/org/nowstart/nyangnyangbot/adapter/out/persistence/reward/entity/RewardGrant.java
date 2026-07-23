package org.nowstart.nyangnyangbot.adapter.out.persistence.reward.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.entity.PointLedgerEntry;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteRound;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardGrantStatus;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

@Entity
@Table(name = "reward_grant")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private UserAccount userAccount;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "roulette_round_id",
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private RouletteRound rouletteRound;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "point_ledger_entry_id",
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private PointLedgerEntry pointLedgerEntry;

    @Column(nullable = false, length = 100, updatable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 32, updatable = false)
    private RewardType rewardType;

    @Enumerated(EnumType.STRING)
    @Column(name = "conversion_mode", nullable = false, length = 16, updatable = false)
    private ConversionMode conversionMode;

    @Column(name = "point_delta", updatable = false)
    private Long pointDelta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RewardGrantStatus status;

    @Column(nullable = false, length = 500, updatable = false)
    private String description;

    @Column(name = "private_note", length = 500, updatable = false)
    private String privateNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "actor_user_id",
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private UserAccount actorUserAccount;

    @Column(name = "idempotency_key", nullable = false, length = 191, updatable = false)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
