package org.nowstart.nyangnyangbot.adapter.out.persistence.timer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;

@Entity
@Table(name = "timer_message")
@DynamicUpdate
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimerMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_template", length = 1_000, nullable = false)
    private String messageTemplate;

    @Column(name = "interval_minutes", nullable = false)
    private Integer intervalMinutes;

    @Column(name = "min_chat_count", nullable = false)
    private Integer minChatCount;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "chat_count_since_last_send", nullable = false)
    private long chatCountSinceLastSend;

    @Column(name = "claimed_chat_count", nullable = false)
    private long claimedChatCount;

    @Column(name = "claim_token", length = 36)
    private String claimToken;

    @Column(name = "claim_expires_at")
    private Instant claimExpiresAt;

    @Column(name = "last_sent_at")
    private Instant lastSentAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private UserAccount createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private UserAccount updatedByUser;

    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp(source = SourceType.DB)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void update(
            String newMessageTemplate,
            Integer newIntervalMinutes,
            Integer newMinChatCount,
            boolean newActive,
            Instant newNextRunAt,
            boolean resetSchedule,
            UserAccount updater
    ) {
        this.messageTemplate = newMessageTemplate;
        this.intervalMinutes = newIntervalMinutes;
        this.minChatCount = newMinChatCount;
        this.active = newActive;
        this.nextRunAt = newNextRunAt;
        this.updatedByUser = updater;
        if (resetSchedule) {
            this.chatCountSinceLastSend = 0;
        }
    }
}
