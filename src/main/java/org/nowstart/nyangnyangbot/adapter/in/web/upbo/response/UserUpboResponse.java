package org.nowstart.nyangnyangbot.adapter.in.web.upbo.response;

import java.time.format.DateTimeFormatter;
import org.nowstart.nyangnyangbot.domain.model.UserUpbo;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;

public record UserUpboResponse(
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

    public static UserUpboResponse from(UserUpbo entity) {
        String createdAt = entity.createdAt() == null ? null : entity.createdAt().format(DATE_FORMATTER);
        return new UserUpboResponse(
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
