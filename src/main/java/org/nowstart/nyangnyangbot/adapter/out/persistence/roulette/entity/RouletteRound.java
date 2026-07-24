package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity;

import static org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy.MAX_FAILURE_REASON_LENGTH;

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
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;

@Entity
@Table(name = "roulette_round")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouletteRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "roulette_run_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private RouletteRun rouletteRun;

    @Column(name = "roulette_config_id", nullable = false, updatable = false)
    private Long rouletteConfigId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "roulette_option_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private RouletteOption rouletteOption;

    @Column(name = "round_no", nullable = false, updatable = false)
    private Integer roundNo;

    @Column(nullable = false, updatable = false)
    private Integer ticket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RouletteRoundStatus status;

    @Column(name = "failure_reason", length = MAX_FAILURE_REASON_LENGTH)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void markApplied(Instant appliedAt) {
        requireConfirmed();
        status = RouletteRoundStatus.APPLIED;
        failureReason = null;
        updatedAt = appliedAt;
    }

    public void markFailed(String reason, Instant failedAt) {
        requireConfirmed();
        status = RouletteRoundStatus.FAILED;
        failureReason = reason;
        updatedAt = failedAt;
    }

    private void requireConfirmed() {
        if (status != RouletteRoundStatus.CONFIRMED) {
            throw new IllegalStateException("roulette round must be CONFIRMED");
        }
    }
}
