package org.nowstart.nyangnyangbot.data.dto.upbo;

import java.time.format.DateTimeFormatter;
import org.nowstart.nyangnyangbot.application.model.UserUpbo;
import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.RewardType;
import org.nowstart.nyangnyangbot.data.type.UpboStatus;

public record UserUpboDto(
        Long id,
        String userId,
        String nickNameSnapshot,
        String label,
        UpboStatus status,
        Integer exchangeFavoriteValue,
        RewardType rewardType,
        ConversionMode conversionMode,
        Long ledgerId,
        String publicDescription,
        String createdAt
) {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static UserUpboDto from(UserUpbo entity) {
        String createdAt = entity.createdAt() == null ? null : entity.createdAt().format(DATE_FORMATTER);
        return new UserUpboDto(
                entity.id(),
                entity.userId(),
                entity.nickNameSnapshot(),
                entity.label(),
                entity.status(),
                entity.exchangeFavoriteValue(),
                entity.rewardType(),
                entity.conversionMode(),
                entity.ledgerId(),
                entity.publicDescription(),
                createdAt
        );
    }
}
