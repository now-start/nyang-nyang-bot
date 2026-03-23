package org.nowstart.nyangnyangbot.data.dto.favorite;

public record FavoriteListItemDto(
        String userId,
        String nickName,
        Integer favorite,
        Integer totalFavorite
) {

    public Integer displayFavorite() {
        if (totalFavorite != null) {
            return totalFavorite;
        }
        return favorite;
    }

    public Integer getDisplayFavorite() {
        return displayFavorite();
    }
}
