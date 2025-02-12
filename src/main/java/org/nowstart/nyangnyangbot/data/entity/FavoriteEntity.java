package org.nowstart.nyangnyangbot.data.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteEntity extends BaseEntity {

    @Id
    private String userId;
    private String nickName;
    private int favorite;
    @OneToMany(mappedBy = "favoriteEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    List<FavoriteHistoryEntity> favoriteHistoryEntityList;

    public FavoriteEntity addFavorite(int addFavorite) {
        this.favorite += addFavorite;
        return this;
    }

    public FavoriteEntity updateNickName(String nickName) {
        this.nickName = nickName;
        return this;
    }
}
