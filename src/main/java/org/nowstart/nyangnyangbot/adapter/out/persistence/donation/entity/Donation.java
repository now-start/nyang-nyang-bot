package org.nowstart.nyangnyangbot.adapter.out.persistence.donation.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;

@Entity
@Table(name = "donation")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ingestion_key", nullable = false, length = 255, updatable = false)
    private String ingestionKey;

    @Column(name = "donation_type", nullable = false, length = 32, updatable = false)
    private String donationType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "recipient_user_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private UserAccount recipientUserAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "donor_user_id",
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private UserAccount donorUserAccount;

    @Column(name = "donor_display_name", length = 100, updatable = false)
    private String donorDisplayName;

    @Column(nullable = false, updatable = false)
    private Long amount;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(columnDefinition = "LONGTEXT", updatable = false)
    private String message;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;
}
