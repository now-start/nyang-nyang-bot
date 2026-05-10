package org.nowstart.nyangnyangbot.application.dto.favorite;

import java.util.List;

public record FavoriteMeDto(
        String userId,
        String nickName,
        Integer favorite,
        Long unseenCount,
        List<FavoriteHistoryDto> histories
) {
}
