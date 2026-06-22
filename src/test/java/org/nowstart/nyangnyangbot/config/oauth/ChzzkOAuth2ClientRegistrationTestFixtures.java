package org.nowstart.nyangnyangbot.config.oauth;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

final class ChzzkOAuth2ClientRegistrationTestFixtures {

    private ChzzkOAuth2ClientRegistrationTestFixtures() {
    }

    static ClientRegistration clientRegistration() {
        return ClientRegistration.withRegistrationId("chzzk")
                .clientId("client-id")
                .clientSecret("client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://spring.nowstart.org/nyang-nyang-bot/login/oauth2/code/chzzk")
                .authorizationUri("https://chzzk.naver.com/account-interlock")
                .tokenUri("https://openapi.chzzk.naver.com/auth/v1/token")
                .userInfoUri("https://openapi.chzzk.naver.com/open/v1/users/me")
                .userNameAttributeName("channelId")
                .clientName("Chzzk")
                .build();
    }
}
