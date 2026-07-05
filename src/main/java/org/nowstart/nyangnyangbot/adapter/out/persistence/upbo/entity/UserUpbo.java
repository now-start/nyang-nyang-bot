package org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.entity;

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
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserUpbo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;
    @ManyToOne
    private UpboTemplate upboTemplate;
    private String nickNameSnapshot;
    private String label;
    @Enumerated(EnumType.STRING)
    private UpboStatus status;
    private Integer exchangeFavoriteValue;
    @Enumerated(EnumType.STRING)
    private RewardType rewardType;
    @Enumerated(EnumType.STRING)
    private ConversionMode conversionMode;
    @Enumerated(EnumType.STRING)
    private FavoriteSourceType sourceType;
    private Long ledgerId;
    private String publicDescription;
    private String privateMemo;
    private String actorId;
}
