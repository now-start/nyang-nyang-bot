package org.nowstart.nyangnyangbot.adapter.in.web.upbo.response;

import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase.UserUpboResult;

public record UpboApplyResponse(
        Long id,
        String userId,
        String label,
        String status,
        Integer exchangeFavoriteValue,
        String conversionMode,
        Long ledgerId,
        String publicDescription
) {

    public static UpboApplyResponse from(UserUpboResult result) {
        return new UpboApplyResponse(
                result.id(),
                result.userId(),
                result.label(),
                result.status(),
                result.exchangeFavoriteValue(),
                result.conversionMode(),
                result.ledgerId(),
                result.publicDescription()
        );
    }
}
