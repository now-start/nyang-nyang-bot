package org.nowstart.nyangnyangbot.data.entity;

import jakarta.persistence.Column;
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
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteHistoryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String history;
    private Integer favorite;
    private Integer delta;
    private Integer balanceAfter;
    @Enumerated(EnumType.STRING)
    private FavoriteSourceType sourceType;
    private String sourceId;
    private String displayCategory;
    private String publicDescription;
    private String privateMemo;
    private Long correctionOfLedgerId;
    private String actorId;
    @Column(unique = true)
    private String idempotencyKey;
    private String nickNameSnapshot;
    @ManyToOne
    private FavoriteEntity favoriteEntity;

}
