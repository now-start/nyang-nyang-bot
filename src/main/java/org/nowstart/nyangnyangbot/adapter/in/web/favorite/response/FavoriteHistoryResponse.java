package org.nowstart.nyangnyangbot.adapter.in.web.favorite.response;

import java.time.format.DateTimeFormatter;
import org.nowstart.nyangnyangbot.application.port.in.favorite.QueryFavoriteUseCase.FavoriteHistoryResult;

public record FavoriteHistoryResponse(
        Long ledgerId,
        String channelId,
        String nickNameSnapshot,
        Integer delta,
        Integer balanceAfter,
        String sourceType,
        String displayCategory,
        String publicDescription,
        Boolean correction,
        Integer favorite,
        String history,
        String date
) {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static FavoriteHistoryResponse from(FavoriteHistoryResult result) {
        String formattedDate = result.createdAt() == null ? null : result.createdAt().format(DATE_FORMATTER);
        return new FavoriteHistoryResponse(
                result.ledgerId(),
                result.channelId(),
                result.nickNameSnapshot(),
                result.delta(),
                result.balanceAfter(),
                result.sourceType(),
                result.displayCategory(),
                result.publicDescription(),
                result.correction(),
                result.favorite(),
                result.history(),
                formattedDate
        );
    }
}
