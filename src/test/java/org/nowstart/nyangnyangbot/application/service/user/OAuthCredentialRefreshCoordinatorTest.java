package org.nowstart.nyangnyangbot.application.service.user;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationToken;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkConfigurationPort;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort.OAuthCredentialRecord;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort.SaveOAuthCredential;

@ExtendWith(MockitoExtension.class)
class OAuthCredentialRefreshCoordinatorTest {

    private static final Instant NOW = Instant.parse("2026-07-23T00:00:00Z");

    @Mock
    private ChzzkConfigurationPort configurationPort;
    @Mock
    private ChzzkClientPort chzzkClientPort;
    @Mock
    private OAuthCredentialPort credentialPort;
    @InjectMocks
    private OAuthCredentialRefreshCoordinator coordinator;

    @Test
    void refreshIfExpired_RechecksCredentialAfterLockAndSkipsProviderWhenAnotherRequestRefreshedIt() {
        OAuthCredentialRecord fresh = credential("fresh", "new-refresh", NOW.plusSeconds(3600), 1);
        given(credentialPort.findByUserIdForUpdate("channel-1")).willReturn(Optional.of(fresh));
        given(credentialPort.currentDatabaseTime()).willReturn(NOW);

        OAuthCredentialRecord result = coordinator.refreshIfExpired("channel-1");

        then(result).isSameAs(fresh);
        verify(chzzkClientPort, never()).getAccessToken(any());
        verify(credentialPort, never()).updateToken(any(), anyLong(), any());
    }

    @Test
    void refreshIfExpired_UsesOneTimeRefreshTokenOnceAndUpdatesObservedLockedVersion() {
        OAuthCredentialRecord expired = credential("old", "one-time-refresh", NOW.minusSeconds(1), 7);
        OAuthCredentialRecord fresh = credential("fresh", "next-refresh", NOW.plusSeconds(3600), 8);
        AuthorizationToken token = new AuthorizationToken(
                "fresh",
                "next-refresh",
                "Bearer",
                3600,
                "scope"
        );
        given(configurationPort.clientId()).willReturn("client");
        given(configurationPort.clientSecret()).willReturn("secret");
        given(credentialPort.findByUserIdForUpdate("channel-1")).willReturn(Optional.of(expired));
        given(credentialPort.currentDatabaseTime()).willReturn(NOW);
        given(chzzkClientPort.getAccessToken(any())).willReturn(token);
        given(credentialPort.updateToken(eq("channel-1"), eq(7L), any())).willReturn(fresh);

        OAuthCredentialRecord result = coordinator.refreshIfExpired("channel-1");

        then(result).isSameAs(fresh);
        ArgumentCaptor<ChzzkClientPort.AuthorizationTokenCommand> refreshCommand =
                ArgumentCaptor.forClass(ChzzkClientPort.AuthorizationTokenCommand.class);
        verify(chzzkClientPort, times(1)).getAccessToken(refreshCommand.capture());
        then(refreshCommand.getValue().refreshToken()).isEqualTo("one-time-refresh");
        verify(chzzkClientPort, never()).getUser(any());
        verify(credentialPort).updateToken(eq("channel-1"), eq(7L), any());
    }

    @Test
    void refreshIfExpired_WhenProviderFailsDoesNotMutateCredential() {
        OAuthCredentialRecord expired = credential("old", "one-time-refresh", NOW.minusSeconds(1), 7);
        given(configurationPort.clientId()).willReturn("client");
        given(configurationPort.clientSecret()).willReturn("secret");
        given(credentialPort.findByUserIdForUpdate("channel-1")).willReturn(Optional.of(expired));
        given(credentialPort.currentDatabaseTime()).willReturn(NOW);
        given(chzzkClientPort.getAccessToken(any())).willThrow(new IllegalStateException("provider unavailable"));

        thenThrownBy(() -> coordinator.refreshIfExpired("channel-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("provider unavailable");
        verify(credentialPort, never()).updateToken(any(), anyLong(), any());
    }

    @Test
    void refreshIfExpired_SecondContenderRechecksLockedFreshStateWithoutReusingRefreshToken() {
        OAuthCredentialRecord expired = credential("old", "one-time-refresh", NOW.minusSeconds(1), 7);
        OAuthCredentialRecord fresh = credential("fresh", "next-refresh", NOW.plusSeconds(3600), 8);
        AuthorizationToken token = new AuthorizationToken(
                "fresh",
                "next-refresh",
                "Bearer",
                3600,
                "scope"
        );
        given(configurationPort.clientId()).willReturn("client");
        given(configurationPort.clientSecret()).willReturn("secret");
        given(credentialPort.findByUserIdForUpdate("channel-1"))
                .willReturn(Optional.of(expired), Optional.of(fresh));
        given(credentialPort.currentDatabaseTime()).willReturn(NOW);
        given(chzzkClientPort.getAccessToken(any())).willReturn(token);
        given(credentialPort.updateToken(eq("channel-1"), eq(7L), any())).willReturn(fresh);

        OAuthCredentialRecord first = coordinator.refreshIfExpired("channel-1");
        OAuthCredentialRecord second = coordinator.refreshIfExpired("channel-1");

        then(first).isSameAs(fresh);
        then(second).isSameAs(fresh);
        verify(chzzkClientPort, times(1)).getAccessToken(any());
        verify(credentialPort, times(1)).updateToken(eq("channel-1"), eq(7L), any());
    }

    private OAuthCredentialRecord credential(
            String accessToken,
            String refreshToken,
            Instant expiresAt,
            long version
    ) {
        return new OAuthCredentialRecord(
                "channel-1",
                "냥이",
                accessToken,
                refreshToken,
                "Bearer",
                true,
                expiresAt,
                version
        );
    }
}
