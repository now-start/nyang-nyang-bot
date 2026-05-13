package org.nowstart.nyangnyangbot.adapter.in.web.upbo.response;

import java.time.format.DateTimeFormatter;
import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase.UserUpboResult;

public record UserUpboResponse(
        Long id,
        String userId,
        String nickNameSnapshot,
        String label,
        String status,
        Integer exchangeFavoriteValue,
        String rewardType,
        String conversionMode,
        Long ledgerId,
        String publicDescription,
        String createdAt
) {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static UserUpboResponse from(UserUpboResult result) {
        String createdAt = result.createdAt() == null ? null : result.createdAt().format(DATE_FORMATTER);
        return new UserUpboResponse(
                result.id(),
                result.userId(),
                result.nickNameSnapshot(),
                result.label(),
                result.status(),
                result.exchangeFavoriteValue(),
                result.rewardType(),
                result.conversionMode(),
                result.ledgerId(),
                result.publicDescription(),
                createdAt
        );
    }
}
