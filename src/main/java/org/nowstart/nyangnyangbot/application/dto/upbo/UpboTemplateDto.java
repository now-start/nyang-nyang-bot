package org.nowstart.nyangnyangbot.application.dto.upbo;

import org.nowstart.nyangnyangbot.domain.model.UpboTemplate;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

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

        public static Response from(UpboTemplate entity) {
            return new Response(
                    entity.id(),
                    entity.label(),
                    entity.description(),
                    entity.active(),
                    entity.displayOrder(),
                    entity.exchangeFavoriteValue(),
                    entity.rewardType(),
                    entity.conversionMode()
            );
        }
    }
}
