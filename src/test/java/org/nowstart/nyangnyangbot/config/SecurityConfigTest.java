package org.nowstart.nyangnyangbot.config;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.service.ChzzkOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
        classes = {
                SecurityConfig.class,
                OAuth2ClientConfig.class,
                ChzzkOAuth2UserService.class,
                WebMvcAutoConfiguration.class,
                JacksonAutoConfiguration.class,
                SecurityAutoConfiguration.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.security.oauth2.client.registration.chzzk.client-id=test",
        "spring.security.oauth2.client.registration.chzzk.client-secret=test",
        "spring.security.oauth2.client.registration.chzzk.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.registration.chzzk.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}",
        "spring.security.oauth2.client.provider.chzzk.authorization-uri=https://example.com/auth",
        "spring.security.oauth2.client.provider.chzzk.token-uri=https://example.com/token",
        "spring.security.oauth2.client.provider.chzzk.user-info-uri=https://example.com/user",
        "spring.security.oauth2.client.provider.chzzk.user-name-attribute=id"
})
class SecurityConfigTest {

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    void securityFilterChain_ShouldBeCreated() {
        then(securityFilterChain).isNotNull();
    }
}






