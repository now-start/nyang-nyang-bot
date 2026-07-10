package org.nowstart.nyangnyangbot.adapter.out.persistence.authorization;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.authorization.entity.AuthorizationAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.authorization.repository.AuthorizationRepository;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort;
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
    public AuthorizationAccountResult saveOrUpdate(SaveAuthorizationCommand command) {
        AuthorizationAccount saved = authorizationRepository.findById(command.channelId())
                .map(existing -> authorizationRepository.save(update(existing, command)))
                .orElseGet(() -> authorizationRepository.save(AuthorizationAccount.builder()
                        .channelId(command.channelId())
                        .channelName(command.channelName())
                        .accessToken(command.accessToken())
                        .refreshToken(command.refreshToken())
                        .tokenType(command.tokenType())
                        .expiresIn(command.expiresIn())
                        .scope(command.scope())
                        .admin(false)
                        .build()));
        return mapper.accountResult(saved);
    }

    @Override
    public AuthorizationAccountResult updateToken(String channelId, SaveAuthorizationCommand command) {
        AuthorizationAccount entity = authorizationRepository.findById(channelId).orElseThrow();
        update(entity, command);
        return mapper.accountResult(entity);
    }

    @Override
    public void markFavoriteHistorySeen(String channelId, LocalDateTime seenAt) {
        authorizationRepository.findById(channelId)
                .ifPresent(entity -> entity.setFavoriteHistoryLastSeenAt(seenAt));
    }

    private AuthorizationAccount update(AuthorizationAccount entity, SaveAuthorizationCommand command) {
        entity.setChannelName(command.channelName());
        entity.setAccessToken(command.accessToken());
        entity.setRefreshToken(command.refreshToken());
        entity.setTokenType(command.tokenType());
        entity.setExpiresIn(command.expiresIn());
        entity.setScope(command.scope());
        return entity;
    }

}
