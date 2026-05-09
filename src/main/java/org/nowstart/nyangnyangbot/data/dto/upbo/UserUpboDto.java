package org.nowstart.nyangnyangbot.data.dto.upbo;

import java.time.format.DateTimeFormatter;
import org.nowstart.nyangnyangbot.data.entity.UserUpboEntity;
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

    public static UserUpboDto from(UserUpboEntity entity) {
        String createdAt = entity.getCreateDate() == null ? null : entity.getCreateDate().format(DATE_FORMATTER);
        return new UserUpboDto(
                entity.getId(),
                entity.getUserId(),
                entity.getNickNameSnapshot(),
                entity.getLabel(),
                entity.getStatus(),
                entity.getExchangeFavoriteValue(),
                entity.getRewardType(),
                entity.getConversionMode(),
                entity.getLedgerId(),
                entity.getPublicDescription(),
                createdAt
        );
    }
}
