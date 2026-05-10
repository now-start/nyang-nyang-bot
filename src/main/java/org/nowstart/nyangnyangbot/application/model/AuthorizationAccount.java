package org.nowstart.nyangnyangbot.application.model;

import java.time.LocalDateTime;

public record AuthorizationAccount(
        String channelId,
        String channelName,
        String accessToken,
        String refreshToken,
        String tokenType,
        Integer expiresIn,
        String scope,
        boolean admin,
        LocalDateTime modifyDate,
        LocalDateTime favoriteHistoryLastSeenAt
) {
}
