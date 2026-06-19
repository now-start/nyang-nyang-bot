package org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.entity;

import org.nowstart.nyangnyangbot.adapter.out.persistence.common.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OverlayTokenEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tokenHash;
    private boolean active;
    private LocalDateTime revokedAt;
    private String issuedBy;

    public void revoke(LocalDateTime revokedAt) {
        active = false;
        this.revokedAt = revokedAt;
    }
}
