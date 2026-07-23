package org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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

@Entity
@Table(name = "user_account")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccount {

    @Id
    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "is_admin", nullable = false)
    private boolean admin;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp(source = SourceType.DB)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void observe(String observedDisplayName) {
        if (observedDisplayName != null && !observedDisplayName.isBlank()) {
            this.displayName = observedDisplayName;
        }
    }

    public void recordLogin(String currentDisplayName, Instant loggedInAt) {
        observe(currentDisplayName);
        this.lastLoginAt = loggedInAt;
    }
}
