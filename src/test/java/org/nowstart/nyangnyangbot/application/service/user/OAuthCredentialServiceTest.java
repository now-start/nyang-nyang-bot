package org.nowstart.nyangnyangbot.application.service.user;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkConfigurationPort;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort.OAuthCredentialRecord;

@ExtendWith(MockitoExtension.class)
class OAuthCredentialServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-23T00:00:00Z");

    @Mock
    private ChzzkConfigurationPort configurationPort;
    @Mock
    private OAuthCredentialPort credentialPort;
    @Mock
    private OAuthCredentialRefreshCoordinator refreshCoordinator;
    @InjectMocks
    private OAuthCredentialService service;

    @Test
    void getAccessToken_WhenCredentialIsStillValidReturnsWithoutTakingRefreshLock() {
        OAuthCredentialRecord fresh = credential("fresh", NOW.plusSeconds(3600), 1);
        given(configurationPort.channelId()).willReturn("channel-1");
        given(credentialPort.findByUserId("channel-1")).willReturn(Optional.of(fresh));
        given(credentialPort.currentDatabaseTime()).willReturn(NOW);

        OAuthCredentialRecord result = service.getAccessToken();

        then(result).isSameAs(fresh);
        verifyNoInteractions(refreshCoordinator);
    }

    @Test
    void getAccessToken_WhenCredentialExpiredDelegatesToLockedRefreshCoordinator() {
        OAuthCredentialRecord expired = credential("old", NOW.minusSeconds(1), 0);
        OAuthCredentialRecord fresh = credential("fresh", NOW.plusSeconds(3600), 1);
        given(configurationPort.channelId()).willReturn("channel-1");
        given(credentialPort.findByUserId("channel-1")).willReturn(Optional.of(expired));
        given(credentialPort.currentDatabaseTime()).willReturn(NOW);
        given(refreshCoordinator.refreshIfExpired("channel-1")).willReturn(fresh);

        OAuthCredentialRecord result = service.getAccessToken();

        then(result).isSameAs(fresh);
    }

    private OAuthCredentialRecord credential(String accessToken, Instant expiresAt, long version) {
        return new OAuthCredentialRecord(
                "channel-1",
                "냥이",
                accessToken,
                "refresh",
                "Bearer",
                "scope",
                true,
                expiresAt,
                version,
                NOW,
                NOW
        );
    }
}
