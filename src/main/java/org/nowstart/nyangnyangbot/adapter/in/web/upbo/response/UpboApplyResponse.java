package org.nowstart.nyangnyangbot.adapter.in.web.upbo.response;

import org.nowstart.nyangnyangbot.domain.model.UserUpbo;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;

public record UpboApplyResponse(
        Long id,
        String userId,
        String label,
        UpboStatus status,
        Integer exchangeFavoriteValue,
        ConversionMode conversionMode,
        Long ledgerId,
        String publicDescription
) {

    public static UpboApplyResponse from(UserUpbo entity) {
        return new UpboApplyResponse(
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
