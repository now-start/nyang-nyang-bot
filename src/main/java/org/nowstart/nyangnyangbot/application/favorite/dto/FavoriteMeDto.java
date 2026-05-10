package org.nowstart.nyangnyangbot.application.favorite.dto;

import java.util.List;

public record FavoriteMeDto(
        String userId,
        String nickName,
        Integer favorite,
        Long unseenCount,
        List<FavoriteHistoryDto> histories
) {
}
