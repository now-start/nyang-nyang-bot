package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.RestOperations;

@ExtendWith(MockitoExtension.class)
class ChzzkOAuth2UserServiceTest {

    @Mock
    private RestOperations restOperations;

    private ChzzkOAuth2UserService userService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        userService = new ChzzkOAuth2UserService(new ObjectMapper(), restOperations);
    }

    @Test
    void loadUser_ShouldReturnUser_WhenResponseValid() {
        OAuth2UserRequest request = userRequest("https://example.com/user", "channelId", "token");
        String body = """
                {"code":0,"message":"OK","content":{"channelId":"chan1","channelName":"test"}}
                """;
        BDDMockito.given(restOperations.exchange(
                BDDMockito.eq("https://example.com/user"),
                BDDMockito.eq(HttpMethod.GET),
                BDDMockito.any(HttpEntity.class),
                BDDMockito.eq(String.class)
        )).willReturn(ResponseEntity.ok(body));

        OAuth2User user = userService.loadUser(request);

        then(user.getName()).isEqualTo("chan1");
        then(user.getAttributes()).containsEntry("channelName", "test");
    }

    @Test
    void loadUser_ShouldThrow_WhenUserInfoMissing() {
        OAuth2UserRequest request = userRequest("", "channelId", "token");

        thenThrownBy(() -> userService.loadUser(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("UserInfo URI missing");
    }

    @Test
    void loadUser_ShouldThrow_WhenContentMissing() {
        OAuth2UserRequest request = userRequest("https://example.com/user", "channelId", "token");
        String body = "{\"code\":0,\"message\":\"OK\",\"content\":null}";
        BDDMockito.given(restOperations.exchange(
                BDDMockito.eq("https://example.com/user"),
                BDDMockito.eq(HttpMethod.GET),
                BDDMockito.any(HttpEntity.class),
                BDDMockito.eq(String.class)
        )).willReturn(ResponseEntity.ok(body));

        thenThrownBy(() -> userService.loadUser(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("UserInfo response missing");
    }

    @Test
    void loadUser_ShouldThrow_WhenResponseNull() {
        OAuth2UserRequest request = userRequest("https://example.com/user", "channelId", "token");
        String body = "null";
        BDDMockito.given(restOperations.exchange(
                BDDMockito.eq("https://example.com/user"),
                BDDMockito.eq(HttpMethod.GET),
                BDDMockito.any(HttpEntity.class),
                BDDMockito.eq(String.class)
        )).willReturn(ResponseEntity.ok(body));

        thenThrownBy(() -> userService.loadUser(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("UserInfo response missing");
    }

    @Test
    void loadUser_ShouldUseDefaultUserNameAttribute_WhenMissing() {
        OAuth2UserRequest request = userRequest("https://example.com/user", "", "token");
        String body = """
                {"code":0,"message":"OK","content":{"channelId":"chan2","channelName":"test2"}}
                """;
        BDDMockito.given(restOperations.exchange(
                BDDMockito.eq("https://example.com/user"),
                BDDMockito.eq(HttpMethod.GET),
                BDDMockito.any(HttpEntity.class),
                BDDMockito.eq(String.class)
        )).willReturn(ResponseEntity.ok(body));

        OAuth2User user = userService.loadUser(request);

        then(user.getName()).isEqualTo("chan2");
        then(user.getAttributes()).containsEntry("channelName", "test2");
    }

    @Test
    void loadUser_ShouldThrow_WhenResponseInvalid() {
        OAuth2UserRequest request = userRequest("https://example.com/user", "channelId", "token");
        String body = "not-json";
        BDDMockito.given(restOperations.exchange(
                BDDMockito.eq("https://example.com/user"),
                BDDMockito.eq(HttpMethod.GET),
                BDDMockito.any(HttpEntity.class),
                BDDMockito.eq(String.class)
        )).willReturn(ResponseEntity.ok(body));

        thenThrownBy(() -> userService.loadUser(request))
                .isInstanceOf(IllegalStateException.class);
    }

    private OAuth2UserRequest userRequest(String userInfoUri, String userNameAttribute, String tokenValue) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("chzzk")
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost")
                .authorizationUri("http://auth")
                .tokenUri("http://token")
                .userInfoUri(userInfoUri)
                .userNameAttributeName(userNameAttribute)
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                tokenValue,
                Instant.now(),
                Instant.now().plusSeconds(300)
        );
        return new OAuth2UserRequest(registration, accessToken);
    }
}






