package org.nowstart.chzzk_favorite_bot.data.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FavoriteEntity {

    @Id
    private String userId;
    private String nickName;
    private int favorite;
    @CreatedDate
    private LocalDateTime createDate;
    @LastModifiedDate
    private LocalDateTime modifyDate;
    @OneToMany(mappedBy = "favoriteEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    List<FavoriteHistoryEntity> favoriteHistoryEntityList;

    @Builder
    public FavoriteEntity(String userId, String nickName, int favorite) {
        this.userId = userId;
        this.nickName = nickName;
        this.favorite = favorite;
    }

    public FavoriteEntity addFavorite(int addFavorite) {
        this.favorite += addFavorite;
        return this;
    }

    public FavoriteEntity updateNickName(String nickName) {
        this.nickName = nickName;
        return this;
    }
}
