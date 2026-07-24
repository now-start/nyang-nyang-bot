package org.nowstart.nyangnyangbot.adapter.out.persistence.user;

import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.OAuthCredential;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.OAuthCredentialRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.UserAccountRepository;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OAuthCredentialPersistenceAdapter implements OAuthCredentialPort {

    private final UserAccountRepository userAccountRepository;
    private final OAuthCredentialRepository credentialRepository;
    private final OutboundContractValidator contractValidator;

    @Override
    @Transactional(readOnly = true)
    public Optional<OAuthCredentialRecord> findByUserId(String userId) {
        return credentialRepository.findById(userId).map(this::record);
    }

    @Override
    @Transactional
    public Optional<OAuthCredentialRecord> findByUserIdForUpdate(String userId) {
        return credentialRepository.findByIdForUpdate(userId).map(this::record);
    }

    @Override
    @Transactional(readOnly = true)
    public Instant currentDatabaseTime() {
        return userAccountRepository.currentDatabaseTime();
    }

    @Override
    @Transactional
    public OAuthCredentialRecord saveLogin(SaveOAuthCredential command) {
        contractValidator.request("oauthCredential.saveLogin", command);
        Instant now = userAccountRepository.currentDatabaseTime();
        userAccountRepository.observe(command.userId(), command.displayName());
        UserAccount account = userAccountRepository.findById(command.userId()).orElseThrow();
        account.recordLogin(command.displayName(), now);
        OAuthCredential credential = credentialRepository.findByIdForUpdate(command.userId())
                .map(existing -> {
                    update(existing, command, now);
                    return existing;
                })
                .orElseGet(() -> OAuthCredential.builder()
                        .userAccount(account)
                        .accessToken(command.accessToken())
                        .refreshToken(command.refreshToken())
                        .tokenType(command.tokenType())
                        .scope(command.scope())
                        .accessTokenExpiresAt(now.plusSeconds(command.expiresIn()))
                        .build());
        return record(credentialRepository.saveAndFlush(credential));
    }

    @Override
    @Transactional
    public OAuthCredentialRecord updateToken(
            String userId,
            long expectedCredentialVersion,
            SaveOAuthCredential command
    ) {
        contractValidator.request("oauthCredential.updateToken", command);
        if (!userId.equals(command.userId())) {
            throw new IllegalArgumentException("OAuth userId does not match the credential owner");
        }
        OAuthCredential credential = credentialRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("OAuth credential not found"));
        if (credential.getCredentialVersion() != expectedCredentialVersion) {
            throw new ObjectOptimisticLockingFailureException(OAuthCredential.class, userId);
        }
        Instant now = userAccountRepository.currentDatabaseTime();
        update(credential, command, now);
        credentialRepository.flush();
        return record(credential);
    }

    private void update(OAuthCredential credential, SaveOAuthCredential command, Instant now) {
        credential.update(
                command.accessToken(),
                command.refreshToken(),
                command.tokenType(),
                command.scope(),
                now.plusSeconds(command.expiresIn())
        );
    }

    private OAuthCredentialRecord record(OAuthCredential credential) {
        UserAccount account = credential.getUserAccount();
        return contractValidator.persistenceResult("oauthCredential.record", new OAuthCredentialRecord(
                account.getUserId(),
                account.getDisplayName(),
                credential.getAccessToken(),
                credential.getRefreshToken(),
                credential.getTokenType(),
                account.isAdmin(),
                credential.getAccessTokenExpiresAt(),
                credential.getCredentialVersion()
        ));
    }
}
