package org.nowstart.nyangnyangbot.data.dto.upbo;

import org.nowstart.nyangnyangbot.data.entity.UpboTemplateEntity;
import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.RewardType;

public class UpboTemplateDto {

    public record CreateRequest(
            String label,
            String description,
            Integer displayOrder,
            Integer exchangeFavoriteValue,
            RewardType rewardType,
            ConversionMode conversionMode
    ) {
    }

    public record Response(
            Long id,
            String label,
            String description,
            Boolean active,
            Integer displayOrder,
            Integer exchangeFavoriteValue,
            RewardType rewardType,
            ConversionMode conversionMode
    ) {

        public static Response from(UpboTemplateEntity entity) {
            return new Response(
                    entity.getId(),
                    entity.getLabel(),
                    entity.getDescription(),
                    entity.isActive(),
                    entity.getDisplayOrder(),
                    entity.getExchangeFavoriteValue(),
                    entity.getRewardType(),
                    entity.getConversionMode()
            );
        }
    }
}
