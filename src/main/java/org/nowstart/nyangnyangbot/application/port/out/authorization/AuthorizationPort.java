package org.nowstart.nyangnyangbot.application.port.out.authorization;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuthorizationPort {

    Optional<AuthorizationAccountResult> findById(String channelId);

    AuthorizationAccountResult saveOrUpdate(SaveAuthorizationCommand command);

    AuthorizationAccountResult updateToken(String channelId, SaveAuthorizationCommand command);

    void markFavoriteHistorySeen(String channelId, LocalDateTime seenAt);

    record SaveAuthorizationCommand(
            String channelId,
            String channelName,
            String accessToken,
            String refreshToken,
            String tokenType,
            Integer expiresIn,
            String scope
    ) {
    }

    record AuthorizationAccountResult(
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
}
