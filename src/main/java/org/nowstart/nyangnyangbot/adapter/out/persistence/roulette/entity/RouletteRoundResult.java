package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.common.BaseEntity;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RouletteRoundResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private RouletteEvent rouletteEvent;
    private Integer roundNo;
    private String itemLabel;
    private Integer probabilityBasisPoints;
    private boolean losingItem;
    @Enumerated(EnumType.STRING)
    private RewardType rewardType;
    @Enumerated(EnumType.STRING)
    private ConversionMode conversionMode;
    private Integer exchangeFavoriteValue;
    @Enumerated(EnumType.STRING)
    private RouletteRoundStatus status;
    private Long ledgerId;
    private Long userUpboId;
    private String failureReason;
    private Integer ticket;

    public void markApplied(Long ledgerId, Long userUpboId) {
        this.status = RouletteRoundStatus.APPLIED;
        this.ledgerId = ledgerId;
        this.userUpboId = userUpboId;
        this.failureReason = null;
    }

    public void markFailed(String failureReason) {
        this.status = RouletteRoundStatus.FAILED;
        this.failureReason = failureReason;
    }
}
