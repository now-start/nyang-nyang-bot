package org.nowstart.nyangnyangbot.application.service.authorization;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import org.mockito.BDDMockito;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
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
    void login_ShouldExchangeAuthorizationCodeAndReturnSavedAccount() {
        // 준비
        AuthorizationToken authorization = new AuthorizationToken("access", "refresh", "Bearer", 3600, "chat");
        UserResult user = new UserResult("channel-1", "tester", "ACTIVE");
        AuthorizationAccountResult saved = new AuthorizationAccountResult(
                "channel-1", "tester", "access", "refresh", "Bearer", 3600, "chat", true, null, null
        );
        given(chzzkProperty.clientId()).willReturn("client-id");
        given(chzzkProperty.clientSecret()).willReturn("client-secret");
        given(chzzkClientPort.getAccessToken(new AuthorizationTokenCommand(
                "authorization_code",
                "client-id",
                "client-secret",
                "code-1",
                "state-1",
                null
        ))).willReturn(new ApiResult<>(200, "OK", authorization));
        given(chzzkClientPort.getUser("Bearer access")).willReturn(new ApiResult<>(200, "OK", user));
        given(authorizationPort.saveOrUpdate(user, authorization)).willReturn(saved);

        // 실행
        var result = authorizationService.login("code-1", "state-1");

        // 검증
        then(result.channelId()).isEqualTo("channel-1");
        then(result.admin()).isTrue();
        BDDMockito.then(authorizationPort).should().saveOrUpdate(user, authorization);
    }

    @Test
    void getAccessToken_ShouldRefreshWhenExpiresInIsNull() {
        // 준비
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

        // 실행
        AuthorizationAccountResult result = authorizationService.getAccessToken();

        // 검증
        then(result.accessToken()).isEqualTo("new-access");
        then(result.refreshToken()).isEqualTo("new-refresh");
        then(result.expiresIn()).isEqualTo(3600);
        then(result.channelName()).isEqualTo("updated-user");
    }

    @Test
    void getAccessToken_ShouldReturnCachedTokenWhenNotExpired() {
        // 준비
        AuthorizationAccountResult account = new AuthorizationAccountResult(
                "channel-1",
                "tester",
                "access",
                "refresh",
                "Bearer",
                3600,
                "chat",
                false,
                LocalDateTime.now().minusSeconds(10),
                null
        );
        given(chzzkProperty.channelId()).willReturn("channel-1");
        given(authorizationPort.findById("channel-1")).willReturn(Optional.of(account));

        // 실행
        AuthorizationAccountResult result = authorizationService.getAccessToken();

        // 검증
        then(result).isSameAs(account);
        BDDMockito.then(chzzkClientPort).shouldHaveNoInteractions();
        BDDMockito.then(authorizationPort).should(never()).updateToken(any(), any(), any());
    }

    @Test
    void getAccessToken_ShouldRefreshWhenModifyDateIsMissing() {
        // 준비
        AuthorizationAccountResult account = new AuthorizationAccountResult(
                "channel-1", "tester", "access", "refresh", "Bearer", 3600, "chat", false, null, null
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

        // 실행
        AuthorizationAccountResult result = authorizationService.getAccessToken();

        // 검증
        then(result.channelName()).isEqualTo("updated-user");
    }

    @Test
    void getAccessToken_ShouldRefreshWhenTokenIsExpired() {
        // 준비
        AuthorizationAccountResult account = new AuthorizationAccountResult(
                "channel-1",
                "tester",
                "access",
                "refresh",
                "Bearer",
                1,
                "chat",
                false,
                LocalDateTime.now().minusSeconds(60),
                null
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

        // 실행
        AuthorizationAccountResult result = authorizationService.getAccessToken();

        // 검증
        then(result.accessToken()).isEqualTo("new-access");
    }
}
