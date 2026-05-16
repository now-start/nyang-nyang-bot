package org.nowstart.nyangnyangbot.adapter.in.web.favorite.response;

import java.util.List;
import org.nowstart.nyangnyangbot.application.port.in.favorite.QueryFavoriteUseCase.FavoriteMeResult;

public record FavoriteMeResponse(
        String userId,
        String nickName,
        Integer favorite,
        Long unseenCount,
        List<FavoriteHistoryResponse> histories
) {

    public static FavoriteMeResponse from(FavoriteMeResult result) {
        return new FavoriteMeResponse(
                result.userId(),
                result.nickName(),
                result.favorite(),
                result.unseenCount(),
                result.histories().stream().map(FavoriteHistoryResponse::from).toList()
        );
    }
}
