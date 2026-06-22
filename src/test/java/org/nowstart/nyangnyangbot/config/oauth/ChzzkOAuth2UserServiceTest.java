package org.nowstart.nyangnyangbot.config.oauth;

import static org.assertj.core.api.BDDAssertions.then;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

class ChzzkOAuth2UserServiceTest {

    @Test
    void loadUser_ShouldCreatePrincipalFromChzzkTokenAdditionalParameters() {
        // 준비
        ChzzkOAuth2UserService userService = new ChzzkOAuth2UserService();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
        OAuth2UserRequest request = new OAuth2UserRequest(
                ChzzkOAuth2ClientRegistrationTestFixtures.clientRegistration(),
                accessToken,
                Map.of(
                        "channel_id", "channel-1",
                        "channel_name", "tester",
                        "admin", true
                )
        );

        // 실행
        var user = userService.loadUser(request);

        // 검증
        then(user.getName()).isEqualTo("channel-1");
        then(user.<String>getAttribute("channelName")).isEqualTo("tester");
        then(user.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }
}
