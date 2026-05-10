package org.nowstart.nyangnyangbot.data.dto.upbo;

import org.nowstart.nyangnyangbot.application.model.UserUpbo;
import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.RewardType;
import org.nowstart.nyangnyangbot.data.type.UpboStatus;

public class UpboApplyDto {

    public record Request(
            String userId,
            String nickName,
            Long templateId,
            String label,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            String publicDescription,
            String privateMemo
    ) {
    }

    public record Response(
            Long id,
            String userId,
            String label,
            UpboStatus status,
            Integer exchangeFavoriteValue,
            ConversionMode conversionMode,
            Long ledgerId,
            String publicDescription
    ) {

        public static Response from(UserUpbo entity) {
            return new Response(
                    entity.id(),
                    entity.userId(),
                    entity.label(),
                    entity.status(),
                    entity.exchangeFavoriteValue(),
                    entity.conversionMode(),
                    entity.ledgerId(),
                    entity.publicDescription()
            );
        }
    }
}
