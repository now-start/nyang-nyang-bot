package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity;

import org.nowstart.nyangnyangbot.adapter.out.persistence.common.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

@Entity
@Table(name = "roulette_item")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RouletteItemEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "roulette_table_id")
    private RouletteTableEntity rouletteTable;
    @Setter
    private String label;
    @Setter
    private Integer probabilityBasisPoints;
    @Setter
    private boolean losingItem;
    @Setter
    @Enumerated(EnumType.STRING)
    private RewardType rewardType;
    @Setter
    @Enumerated(EnumType.STRING)
    private ConversionMode conversionMode;
    @Setter
    private Integer exchangeFavoriteValue;
    @Setter
    private boolean active;
    @Setter
    private Integer displayOrder;
}
