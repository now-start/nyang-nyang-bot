package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChzzkOAuth2TokenServiceTest {

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @InjectMocks
    private ChzzkOAuth2TokenService tokenService;

    private void setDefaults() {
        ReflectionTestUtils.setField(tokenService, "registrationId", "chzzk");
        ReflectionTestUtils.setField(tokenService, "principalName", "tester");
    }

    @Test
    void getAccessTokenValue_ShouldReturnToken() {
        setDefaults();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "token123",
                Instant.now(),
                Instant.now().plusSeconds(300)
        );
        OAuth2AuthorizedClient client = BDDMockito.mock(OAuth2AuthorizedClient.class);
        BDDMockito.given(client.getAccessToken()).willReturn(accessToken);
        BDDMockito.given(authorizedClientManager.authorize(BDDMockito.any(OAuth2AuthorizeRequest.class))).willReturn(client);

        String token = tokenService.getAccessTokenValue();

        then(token).isEqualTo("token123");
    }

    @Test
    void getAccessTokenValue_ShouldThrow_WhenClientMissing() {
        setDefaults();
        BDDMockito.given(authorizedClientManager.authorize(BDDMockito.any(OAuth2AuthorizeRequest.class))).willReturn(null);

        thenThrownBy(() -> tokenService.getAccessTokenValue())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OAuth2 authorized client not available");
    }

    @Test
    void getAccessTokenValue_ShouldThrow_WhenTokenMissing() {
        setDefaults();
        OAuth2AuthorizedClient client = BDDMockito.mock(OAuth2AuthorizedClient.class);
        BDDMockito.given(client.getAccessToken()).willReturn(null);
        BDDMockito.given(authorizedClientManager.authorize(BDDMockito.any(OAuth2AuthorizeRequest.class))).willReturn(client);

        thenThrownBy(() -> tokenService.getAccessTokenValue())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OAuth2 authorized client not available");
    }
}






