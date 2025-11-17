package org.nowstart.nyangnyangbot.data.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteEntity extends BaseEntity {

    @Id
    private String userId;
    private String nickName;
    private Integer favorite;
    @Builder.Default
    @OrderBy("createDate DESC")
    @OneToMany(mappedBy = "favoriteEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FavoriteHistoryEntity> favoriteHistoryEntityList = new ArrayList<>();
}
