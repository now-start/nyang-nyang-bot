package org.nowstart.nyangnyangbot.application.service.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort.AuthorizationAccountResult;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.ApiResult;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationToken;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationTokenCommand;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.UserResult;
import org.nowstart.nyangnyangbot.config.property.ChzzkProperty;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private ChzzkProperty chzzkProperty;

    @Mock
    private ChzzkClientPort chzzkClientPort;

    @Mock
    private AuthorizationPort authorizationPort;

    @Spy
    @InjectMocks
    private AuthorizationService authorizationService;

    @Test
    void getAccessToken_ShouldRefreshWhenExpiresInIsNull() {
        AuthorizationAccountResult account = new AuthorizationAccountResult(
                "channel-1", "tester", "access", "refresh", "Bearer", null, "chat", false, null, null
        );
        AuthorizationToken refreshedAuthorization = new AuthorizationToken("new-access", "new-refresh", "Bearer", 3600, "chat");
        UserResult refreshedUser = new UserResult("channel-1", "updated-user", "ACTIVE");
        AuthorizationAccountResult refreshed = new AuthorizationAccountResult(
                "channel-1", "updated-user", "new-access", "new-refresh", "Bearer", 3600, "chat", false, null, null
        );

        given(chzzkProperty.channelId()).willReturn("channel-1");
        given(chzzkProperty.clientId()).willReturn("client-id");
        given(chzzkProperty.clientSecret()).willReturn("client-secret");
        given(authorizationPort.findById("channel-1")).willReturn(Optional.of(account));
        given(chzzkClientPort.getAccessToken(new AuthorizationTokenCommand(
                "refresh_token",
                "client-id",
                "client-secret",
                null,
                null,
                "refresh"
        ))).willReturn(new ApiResult<>(200, "OK", refreshedAuthorization));
        given(chzzkClientPort.getUser("Bearer new-access")).willReturn(new ApiResult<>(200, "OK", refreshedUser));
        given(authorizationPort.updateToken("channel-1", refreshedUser, refreshedAuthorization)).willReturn(refreshed);

        AuthorizationAccountResult result = authorizationService.getAccessToken();

        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
        assertThat(result.expiresIn()).isEqualTo(3600);
        assertThat(result.channelName()).isEqualTo("updated-user");
    }
}
