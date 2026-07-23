package org.nowstart.nyangnyangbot.adapter.out.persistence.command.entity;

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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;
import org.nowstart.nyangnyangbot.domain.command.CommandExecutionPolicy;

@Entity
@Table(name = "command")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Command {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trigger_token", length = 20, nullable = false)
    private String triggerToken;

    @Column(name = "message_template", length = 1_000, nullable = false)
    private String messageTemplate;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_policy", length = 32, nullable = false)
    private CommandExecutionPolicy executionPolicy;

    @Column(name = "user_cooldown_seconds")
    private Integer userCooldownSeconds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by_user_id",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private UserAccount createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "updated_by_user_id",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private UserAccount updatedByUser;

    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp(source = SourceType.DB)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void update(
            String newTriggerToken,
            String newMessageTemplate,
            boolean newActive,
            CommandExecutionPolicy newExecutionPolicy,
            Integer newUserCooldownSeconds,
            UserAccount updater
    ) {
        this.triggerToken = newTriggerToken;
        this.messageTemplate = newMessageTemplate;
        this.active = newActive;
        this.executionPolicy = newExecutionPolicy;
        this.userCooldownSeconds = newUserCooldownSeconds;
        this.updatedByUser = updater;
    }
}
