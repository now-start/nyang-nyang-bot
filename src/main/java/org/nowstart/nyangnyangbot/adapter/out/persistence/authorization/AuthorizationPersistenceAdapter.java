package org.nowstart.nyangnyangbot.adapter.out.persistence.authorization;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.authorization.entity.AuthorizationAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.authorization.repository.AuthorizationRepository;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationToken;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.UserResult;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorizationPersistenceAdapter implements AuthorizationPort {

    private final AuthorizationRepository authorizationRepository;
    private final AuthorizationPersistenceMapper mapper;

    @Override
    public Optional<AuthorizationAccountResult> findById(String channelId) {
        return authorizationRepository.findById(channelId).map(mapper::accountResult);
    }

    @Override
    public AuthorizationAccountResult saveOrUpdate(UserResult user, AuthorizationToken authorization) {
        AuthorizationAccount saved = authorizationRepository.findById(user.channelId())
                .map(existing -> authorizationRepository.save(update(existing, user, authorization)))
                .orElseGet(() -> authorizationRepository.save(AuthorizationAccount.builder()
                        .channelId(user.channelId())
                        .channelName(user.channelName())
                        .accessToken(authorization.accessToken())
                        .refreshToken(authorization.refreshToken())
                        .tokenType(authorization.tokenType())
                        .expiresIn(authorization.expiresIn())
                        .scope(authorization.scope())
                        .admin(false)
                        .build()));
        return mapper.accountResult(saved);
    }

    @Override
    public AuthorizationAccountResult updateToken(String channelId, UserResult user, AuthorizationToken authorization) {
        AuthorizationAccount entity = authorizationRepository.findById(channelId).orElseThrow();
        update(entity, user, authorization);
        return mapper.accountResult(entity);
    }

    @Override
    public void markFavoriteHistorySeen(String channelId, LocalDateTime seenAt) {
        authorizationRepository.findById(channelId)
                .ifPresent(entity -> entity.setFavoriteHistoryLastSeenAt(seenAt));
    }

    private AuthorizationAccount update(AuthorizationAccount entity, UserResult user, AuthorizationToken authorization) {
        entity.setChannelName(user.channelName());
        entity.setAccessToken(authorization.accessToken());
        entity.setRefreshToken(authorization.refreshToken());
        entity.setTokenType(authorization.tokenType());
        entity.setExpiresIn(authorization.expiresIn());
        entity.setScope(authorization.scope());
        return entity;
    }

}
