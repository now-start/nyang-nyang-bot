package org.nowstart.nyangnyangbot.data.dto.favorite;

import java.time.format.DateTimeFormatter;
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;

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

    public static FavoriteHistoryDto from(FavoriteHistoryEntity entity) {
        String formattedDate = entity.getCreateDate() == null ? null : entity.getCreateDate().format(DATE_FORMATTER);
        String channelId = entity.getFavoriteEntity() == null ? null : entity.getFavoriteEntity().getUserId();
        String sourceType = entity.getSourceType() == null ? null : entity.getSourceType().name();
        Integer balanceAfter = entity.getBalanceAfter() == null ? entity.getFavorite() : entity.getBalanceAfter();
        String publicDescription = entity.getPublicDescription() == null ? entity.getHistory() : entity.getPublicDescription();
        return new FavoriteHistoryDto(
                entity.getId(),
                channelId,
                entity.getNickNameSnapshot(),
                entity.getDelta(),
                balanceAfter,
                sourceType,
                entity.getDisplayCategory(),
                publicDescription,
                entity.getCorrectionOfLedgerId() != null,
                entity.getFavorite(),
                entity.getHistory(),
                formattedDate
        );
    }
}
