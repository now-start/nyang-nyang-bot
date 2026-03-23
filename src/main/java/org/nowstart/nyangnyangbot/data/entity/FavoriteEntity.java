package org.nowstart.nyangnyangbot.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteEntity extends BaseEntity {

    @Id
    private String userId;
    @Setter
    private String nickName;
    @Setter
    private Integer totalFavorite;
    @Setter
    private Integer karmaScore;
    @Setter
    private Integer attendanceCount;

    public Integer getFavorite() {
        return totalFavorite;
    }

    public void setFavorite(Integer favorite) {
        this.totalFavorite = favorite;
    }

    public int safeFavorite() {
        return Objects.requireNonNullElse(totalFavorite, 0);
    }

    public int safeKarmaScore() {
        return Objects.requireNonNullElse(karmaScore, 0);
    }

    public int safeAttendanceCount() {
        return Objects.requireNonNullElse(attendanceCount, 0);
    }
}
