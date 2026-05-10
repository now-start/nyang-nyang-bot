package org.nowstart.nyangnyangbot.application.gateway.out.authorization;

import java.time.LocalDateTime;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.model.AuthorizationAccount;
import org.nowstart.nyangnyangbot.application.chzzk.dto.AuthorizationDto;
import org.nowstart.nyangnyangbot.application.chzzk.dto.UserDto;

public interface AuthorizationPort {

    Optional<AuthorizationAccount> findById(String channelId);

    AuthorizationAccount saveOrUpdate(UserDto user, AuthorizationDto authorization);

    AuthorizationAccount updateToken(String channelId, UserDto user, AuthorizationDto authorization);

    void markFavoriteHistorySeen(String channelId, LocalDateTime seenAt);
}
