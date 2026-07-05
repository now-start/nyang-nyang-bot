package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.common.BaseEntity;
import org.nowstart.nyangnyangbot.domain.type.RouletteEventStatus;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RouletteEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String donationEventId;
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
