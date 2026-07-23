package org.nowstart.nyangnyangbot.config.oauth;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import java.util.Map;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort.OAuthCredentialRecord;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort.SaveOAuthCredential;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationToken;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationTokenCommand;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.UserResult;
import org.nowstart.nyangnyangbot.config.property.ChzzkProperty;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;

@ExtendWith(MockitoExtension.class)
class ChzzkOAuth2AccessTokenResponseClientTest {

    @Mock
    private ChzzkProperty chzzkProperty;

    @Mock
    private ChzzkClientPort chzzkClientPort;

    @Mock
    private OAuthCredentialPort credentialPort;

    @Test
    void getTokenResponse_ShouldExchangeChzzkCodeAndPersistAuthorizationAccount() {
        // 준비
        ChzzkOAuth2AccessTokenResponseClient client =
                new ChzzkOAuth2AccessTokenResponseClient(chzzkProperty, chzzkClientPort, credentialPort);
        AuthorizationToken token = new AuthorizationToken("access", "refresh", "Bearer", 3600, "chat");
        UserResult user = new UserResult("channel-1", "tester", "ACTIVE");
        OAuthCredentialRecord saved = new OAuthCredentialRecord(
                "channel-1", "tester", "access", "refresh", "Bearer", "chat", true,
                Instant.now().plusSeconds(3600), 0, Instant.now(), Instant.now()
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
        ))).willReturn(token);
        given(chzzkClientPort.getUser("Bearer access")).willReturn(user);
        SaveOAuthCredential saveCommand = saveCommand(user, token);
        given(credentialPort.saveLogin(saveCommand)).willReturn(saved);

        // 실행
        var response = client.getTokenResponse(grantRequest());

        // 검증
        then(response.getAccessToken().getTokenValue()).isEqualTo("access");
        then(response.getRefreshToken().getTokenValue()).isEqualTo("refresh");
        then(response.getAccessToken().getExpiresAt()).isNotNull();
        then(response.getAdditionalParameters()).containsAllEntriesOf(Map.of(
                "channel_id", "channel-1",
                "channel_name", "tester",
                "admin", true
        ));
        BDDMockito.then(credentialPort).should().saveLogin(saveCommand);
    }

    private OAuth2AuthorizationCodeGrantRequest grantRequest() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://chzzk.naver.com/account-interlock")
                .clientId("client-id")
                .redirectUri("https://spring.nowstart.org/nyang-nyang-bot/login/oauth2/code/chzzk")
                .state("state-1")
                .build();
        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success("code-1")
                .redirectUri("https://spring.nowstart.org/nyang-nyang-bot/login/oauth2/code/chzzk")
                .state("state-1")
                .build();
        return new OAuth2AuthorizationCodeGrantRequest(
                ChzzkOAuth2ClientRegistrationTestFixtures.clientRegistration(),
                new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse)
        );
    }

    private SaveOAuthCredential saveCommand(UserResult user, AuthorizationToken token) {
        return new SaveOAuthCredential(
                user.channelId(),
                user.channelName(),
                token.accessToken(),
                token.refreshToken(),
                token.tokenType(),
                token.expiresIn(),
                token.scope()
        );
    }
}
