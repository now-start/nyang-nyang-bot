package org.nowstart.nyangnyangbot.application.port.out.authorization;

import java.time.LocalDateTime;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationToken;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.UserResult;

public interface AuthorizationPort {

    Optional<AuthorizationAccountResult> findById(String channelId);

    AuthorizationAccountResult saveOrUpdate(UserResult user, AuthorizationToken authorization);

    AuthorizationAccountResult updateToken(String channelId, UserResult user, AuthorizationToken authorization);

    void markFavoriteHistorySeen(String channelId, LocalDateTime seenAt);

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
