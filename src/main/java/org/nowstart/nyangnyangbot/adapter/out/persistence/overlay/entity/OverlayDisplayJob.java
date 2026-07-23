package org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.entity;

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
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteRun;
import org.nowstart.nyangnyangbot.domain.type.OverlayDisplayStatus;

@Entity
@Table(name = "overlay_display_job")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OverlayDisplayJob {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "replay_of_job_id",
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private OverlayDisplayJob replayOfJob;

    @Column(name = "idempotency_key", nullable = false, length = 191, updatable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OverlayDisplayStatus status;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "claim_token", length = 36)
    private String claimToken;

    @Column(name = "claim_expires_at")
    private Instant claimExpiresAt;

    @Column(name = "displayed_at")
    private Instant displayedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void claim(String token, Instant claimedAt, Instant leaseExpiresAt) {
        if (!claimableAt(claimedAt)) {
            throw new IllegalStateException("overlay display job is not claimable");
        }
        if (!leaseExpiresAt.isAfter(claimedAt)) {
            throw new IllegalArgumentException("claim lease must expire after claim time");
        }
        status = OverlayDisplayStatus.DISPLAYING;
        claimToken = token;
        claimExpiresAt = leaseExpiresAt;
        displayedAt = null;
        updatedAt = claimedAt;
    }

    public void markDisplayed(String token, Instant completionTime) {
        if (status != OverlayDisplayStatus.DISPLAYING
                || claimToken == null
                || !claimToken.equals(token)) {
            throw new IllegalArgumentException("overlay display claim token does not match");
        }
        if (!claimExpiresAt.isAfter(completionTime)) {
            throw new IllegalStateException("overlay display claim has expired");
        }
        if (!expiresAt.isAfter(completionTime)) {
            throw new IllegalStateException("overlay display job has expired");
        }
        status = OverlayDisplayStatus.DISPLAYED;
        claimToken = null;
        claimExpiresAt = null;
        displayedAt = completionTime;
        updatedAt = completionTime;
    }

    public boolean claimableAt(Instant now) {
        if (!expiresAt.isAfter(now)) {
            return false;
        }
        return status == OverlayDisplayStatus.PENDING
                || (status == OverlayDisplayStatus.DISPLAYING
                && claimExpiresAt != null
                && !claimExpiresAt.isAfter(now));
    }
}
