package org.nowstart.nyangnyangbot.application.favorite.dto;

import java.time.format.DateTimeFormatter;
import org.nowstart.nyangnyangbot.domain.model.FavoriteHistoryView;

public record FavoriteHistoryDto(
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

    public static FavoriteHistoryDto from(FavoriteHistoryView view) {
        String formattedDate = view.createdAt() == null ? null : view.createdAt().format(DATE_FORMATTER);
        String sourceType = view.sourceType() == null ? null : view.sourceType().name();
        return new FavoriteHistoryDto(
                view.ledgerId(),
                view.channelId(),
                view.nickNameSnapshot(),
                view.delta(),
                view.balanceAfter(),
                sourceType,
                view.displayCategory(),
                view.publicDescription(),
                view.correction(),
                view.favorite(),
                view.history(),
                formattedDate
        );
    }
}
