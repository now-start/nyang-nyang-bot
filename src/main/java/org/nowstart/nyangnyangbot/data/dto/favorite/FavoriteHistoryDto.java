package org.nowstart.nyangnyangbot.data.dto.favorite;

import java.time.format.DateTimeFormatter;
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;

public record FavoriteHistoryDto(
        Integer favorite,
        String history,
        String date
) {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static FavoriteHistoryDto from(FavoriteHistoryEntity entity) {
        String formattedDate = entity.getCreateDate() == null ? null : entity.getCreateDate().format(DATE_FORMATTER);
        return new FavoriteHistoryDto(entity.getFavorite(), entity.getHistory(), formattedDate);
    }
}
