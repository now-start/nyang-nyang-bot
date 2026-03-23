package org.nowstart.nyangnyangbot.data.entity;

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
import org.nowstart.nyangnyangbot.data.type.FavoriteKarmaSourceType;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserKarmaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private FavoriteEntity favoriteEntity;

    @ManyToOne
    private FavoriteAdjustmentEntity adjustment;

    private Integer quantity;
    private String label;
    private Integer amount;

    @Enumerated(EnumType.STRING)
    private FavoriteKarmaSourceType sourceType;
}
