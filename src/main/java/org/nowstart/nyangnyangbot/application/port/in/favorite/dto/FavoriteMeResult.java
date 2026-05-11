package org.nowstart.nyangnyangbot.application.port.in.favorite.dto;

import java.util.List;
import org.nowstart.nyangnyangbot.domain.model.FavoriteHistoryView;

public record FavoriteMeResult(
        String userId,
        String nickName,
        Integer favorite,
        Long unseenCount,
        List<FavoriteHistoryView> histories
) {
}
