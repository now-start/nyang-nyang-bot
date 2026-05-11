package org.nowstart.nyangnyangbot.adapter.out.persistence.authorization;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.domain.model.AuthorizationAccount;
import org.nowstart.nyangnyangbot.application.port.out.authorization.repository.AuthorizationPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.dto.AuthorizationDto;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.dto.UserDto;
import org.nowstart.nyangnyangbot.adapter.out.persistence.entity.AuthorizationEntity;
import org.nowstart.nyangnyangbot.adapter.out.persistence.repository.AuthorizationRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorizationPersistenceAdapter implements AuthorizationPort {

    private final AuthorizationRepository authorizationRepository;

    @Override
    public Optional<AuthorizationAccount> findById(String channelId) {
        return authorizationRepository.findById(channelId).map(this::toModel);
    }

    @Override
    public AuthorizationAccount saveOrUpdate(UserDto user, AuthorizationDto authorization) {
        AuthorizationEntity saved = authorizationRepository.findById(user.channelId())
                .map(existing -> update(existing, user, authorization))
                .orElseGet(() -> authorizationRepository.save(AuthorizationEntity.builder()
                        .channelId(user.channelId())
                        .channelName(user.channelName())
                        .accessToken(authorization.accessToken())
                        .refreshToken(authorization.refreshToken())
                        .tokenType(authorization.tokenType())
                        .expiresIn(authorization.expiresIn())
                        .scope(authorization.scope())
                        .admin(false)
                        .build()));
        return toModel(saved);
    }

    @Override
    public AuthorizationAccount updateToken(String channelId, UserDto user, AuthorizationDto authorization) {
        AuthorizationEntity entity = authorizationRepository.findById(channelId).orElseThrow();
        update(entity, user, authorization);
        return toModel(entity);
    }

    @Override
    public void markFavoriteHistorySeen(String channelId, LocalDateTime seenAt) {
        authorizationRepository.findById(channelId)
                .ifPresent(entity -> entity.setFavoriteHistoryLastSeenAt(seenAt));
    }

    private AuthorizationEntity update(AuthorizationEntity entity, UserDto user, AuthorizationDto authorization) {
        entity.setChannelName(user.channelName());
        entity.setAccessToken(authorization.accessToken());
        entity.setRefreshToken(authorization.refreshToken());
        entity.setTokenType(authorization.tokenType());
        entity.setExpiresIn(authorization.expiresIn());
        entity.setScope(authorization.scope());
        return entity;
    }

    private AuthorizationAccount toModel(AuthorizationEntity entity) {
        return new AuthorizationAccount(
                entity.getChannelId(),
                entity.getChannelName(),
                entity.getAccessToken(),
                entity.getRefreshToken(),
                entity.getTokenType(),
                entity.getExpiresIn(),
                entity.getScope(),
                entity.isAdmin(),
                entity.getModifyDate(),
                entity.getFavoriteHistoryLastSeenAt()
        );
    }
}
