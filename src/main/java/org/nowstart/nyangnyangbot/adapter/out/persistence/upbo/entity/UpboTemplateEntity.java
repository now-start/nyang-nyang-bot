package org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.entity;

import org.nowstart.nyangnyangbot.adapter.out.persistence.common.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpboTemplateEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Setter
    private String label;
    @Setter
    private String description;
    @Setter
    private boolean active;
    @Setter
    private Integer displayOrder;
    @Setter
    private Integer exchangeFavoriteValue;
    @Setter
    @Enumerated(EnumType.STRING)
    private RewardType rewardType;
    @Setter
    @Enumerated(EnumType.STRING)
    private ConversionMode conversionMode;
}
