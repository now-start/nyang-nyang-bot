package org.nowstart.nyangnyangbot.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;

@Entity
@Table(
        name = "roulette_round_result",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_roulette_round_result_event_round",
                columnNames = {"roulette_event_id", "round_no"}
        )
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RouletteRoundResultEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "roulette_event_id")
    private RouletteEventEntity rouletteEvent;
    @Column(name = "round_no")
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
