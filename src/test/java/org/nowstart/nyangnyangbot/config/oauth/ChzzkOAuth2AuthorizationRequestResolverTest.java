package org.nowstart.nyangnyangbot.config.oauth;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

class ChzzkOAuth2AuthorizationRequestResolverTest {

    @Test
    void resolve_ShouldUseChzzkParameterNamesInAuthorizationRequestUri() {
        // 준비
        InMemoryClientRegistrationRepository repository = new InMemoryClientRegistrationRepository(
                ChzzkOAuth2ClientRegistrationTestFixtures.clientRegistration()
        );
        ChzzkOAuth2AuthorizationRequestResolver resolver =
                new ChzzkOAuth2AuthorizationRequestResolver(repository);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/chzzk");
        request.setScheme("https");
        request.setServerName("spring.nowstart.org");
        request.setServerPort(443);

        // 실행
        OAuth2AuthorizationRequest authorizationRequest = resolver.resolve(request);

        // 검증
        then(authorizationRequest).isNotNull();
        then(authorizationRequest.getAuthorizationRequestUri())
                .startsWith("https://chzzk.naver.com/account-interlock?")
                .contains("clientId=client-id")
                .contains("redirectUri=https%3A%2F%2Fspring.nowstart.org%2Fnyang-nyang-bot%2Flogin%2Foauth2%2Fcode%2Fchzzk")
                .contains("state=")
                .doesNotContain("client_id")
                .doesNotContain("redirect_uri");
    }
}
