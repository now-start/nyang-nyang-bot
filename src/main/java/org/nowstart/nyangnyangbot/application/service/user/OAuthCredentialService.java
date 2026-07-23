package org.nowstart.nyangnyangbot.application.service.user;

import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.user.GetOAuthAccessTokenUseCase;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkConfigurationPort;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort.OAuthCredentialRecord;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthCredentialService implements GetOAuthAccessTokenUseCase {

    private final ChzzkConfigurationPort chzzkConfigurationPort;
    private final OAuthCredentialPort credentialPort;
    private final OAuthCredentialRefreshCoordinator refreshCoordinator;

    @Override
    public OAuthCredentialRecord getAccessToken() {
        String userId = chzzkConfigurationPort.channelId();
        OAuthCredentialRecord credential = credentialPort.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("OAuth credential not found"));
        if (credential.accessTokenExpiresAt() != null
                && credential.accessTokenExpiresAt().isAfter(credentialPort.currentDatabaseTime())) {
            return credential;
        }
        return refreshCoordinator.refreshIfExpired(userId);
    }
}
