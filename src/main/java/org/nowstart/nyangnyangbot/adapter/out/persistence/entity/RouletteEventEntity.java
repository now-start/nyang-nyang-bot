package org.nowstart.nyangnyangbot.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.nowstart.nyangnyangbot.domain.type.RouletteEventStatus;

@Entity
@Table(
        name = "roulette_event",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_roulette_event_donation_event_id", columnNames = "donation_event_id"),
                @UniqueConstraint(name = "uk_roulette_event_idempotency_key", columnNames = "idempotency_key")
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RouletteEventEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "donation_event_id", nullable = false)
    private String donationEventId;
    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;
    private String userId;
    private String nickNameSnapshot;
    private Long donationAmount;
    @Lob
    private String donationText;
    private Long rouletteTableId;
    private Integer rouletteTableVersion;
    private String command;
    private Long pricePerRound;
    private Integer roundCount;
    @Lob
    private String itemsSnapshotJson;
    @Enumerated(EnumType.STRING)
    private RouletteEventStatus status;

    public void updateStatus(RouletteEventStatus status) {
        this.status = status;
    }
}
