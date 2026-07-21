package org.nowstart.nyangnyangbot.adapter.out.persistence.authorization;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.authorization.entity.AuthorizationAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.authorization.repository.AuthorizationRepository;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorizationPersistenceAdapter implements AuthorizationPort {

    private final AuthorizationRepository authorizationRepository;
    private final OutboundContractValidator contractValidator;

    @Override
    public Optional<AuthorizationAccountResult> findById(String channelId) {
        return authorizationRepository.findById(channelId).map(this::accountResult);
    }

    @Override
    public AuthorizationAccountResult saveOrUpdate(SaveAuthorizationCommand command) {
        contractValidator.request("authorization.saveOrUpdate", command);
        LocalDateTime loggedInAt = LocalDateTime.now();
        AuthorizationAccount saved = authorizationRepository.findById(command.channelId())
                .map(existing -> {
                    update(existing, command);
                    existing.setLastLoginAt(loggedInAt);
                    return authorizationRepository.save(existing);
                })
                .orElseGet(() -> authorizationRepository.save(AuthorizationAccount.builder()
                        .channelId(command.channelId())
                        .channelName(command.channelName())
                        .accessToken(command.accessToken())
                        .refreshToken(command.refreshToken())
                        .tokenType(command.tokenType())
                        .expiresIn(command.expiresIn())
                        .scope(command.scope())
                        .admin(false)
                        .lastLoginAt(loggedInAt)
                        .build()));
        return accountResult(saved);
    }

    @Override
    public AuthorizationAccountResult updateToken(String channelId, SaveAuthorizationCommand command) {
        contractValidator.request("authorization.updateToken", command);
        AuthorizationAccount entity = authorizationRepository.findById(channelId).orElseThrow();
        update(entity, command);
        return accountResult(entity);
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

    private AuthorizationAccountResult accountResult(AuthorizationAccount entity) {
        return contractValidator.persistenceResult("authorization.accountResult", new AuthorizationAccountResult(
                entity.getChannelId(),
                entity.getChannelName(),
                entity.getAccessToken(),
                entity.getRefreshToken(),
                entity.getTokenType(),
                entity.getExpiresIn(),
                entity.getScope(),
                entity.isAdmin(),
                entity.getModifyDate(),
                entity.getLastLoginAt()
        ));
    }

}
