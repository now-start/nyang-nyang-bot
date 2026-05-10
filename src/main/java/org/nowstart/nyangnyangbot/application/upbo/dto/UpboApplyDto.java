package org.nowstart.nyangnyangbot.application.upbo.dto;

import org.nowstart.nyangnyangbot.domain.model.UserUpbo;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;

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
