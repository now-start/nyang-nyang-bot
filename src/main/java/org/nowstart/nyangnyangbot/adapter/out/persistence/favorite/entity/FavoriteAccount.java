package org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nowstart.nyangnyangbot.adapter.out.persistence.common.BaseEntity;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteAccount extends BaseEntity {

    @Id
    private String userId;
    @Setter
    private String nickName;
    @Setter
    private Integer favorite;
}
