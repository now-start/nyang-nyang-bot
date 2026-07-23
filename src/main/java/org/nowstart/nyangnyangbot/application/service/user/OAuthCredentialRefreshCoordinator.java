package org.nowstart.nyangnyangbot.application.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationToken;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationTokenCommand;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkConfigurationPort;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort.OAuthCredentialRecord;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort.SaveOAuthCredential;
import org.nowstart.nyangnyangbot.domain.type.GrantType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthCredentialRefreshCoordinator {

    private final ChzzkConfigurationPort chzzkConfigurationPort;
    private final ChzzkClientPort chzzkClientPort;
    private final OAuthCredentialPort credentialPort;

    @Transactional
    public OAuthCredentialRecord refreshIfExpired(String userId) {
        OAuthCredentialRecord lockedCredential = credentialPort.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("OAuth credential not found"));
        if (isValid(lockedCredential)) {
            return lockedCredential;
        }

        AuthorizationToken refreshed = chzzkClientPort.getAccessToken(new AuthorizationTokenCommand(
                GrantType.REFRESH_TOKEN.getData(),
                chzzkConfigurationPort.clientId(),
                chzzkConfigurationPort.clientSecret(),
                null,
                null,
                lockedCredential.refreshToken()
        ));
        OAuthCredentialRecord saved = credentialPort.updateToken(
                userId,
                lockedCredential.credentialVersion(),
                saveCommand(lockedCredential, refreshed)
        );
        log.info("event=oauth.token.refreshed userId={}", userId);
        return saved;
    }

    private boolean isValid(OAuthCredentialRecord credential) {
        return credential.accessTokenExpiresAt() != null
                && credential.accessTokenExpiresAt().isAfter(credentialPort.currentDatabaseTime());
    }

    private SaveOAuthCredential saveCommand(OAuthCredentialRecord credential, AuthorizationToken token) {
        return new SaveOAuthCredential(
                credential.userId(),
                credential.displayName(),
                token.accessToken(),
                token.refreshToken(),
                token.tokenType(),
                token.expiresIn(),
                token.scope()
        );
    }
}
