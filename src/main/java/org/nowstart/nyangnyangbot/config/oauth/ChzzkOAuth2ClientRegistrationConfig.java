package org.nowstart.nyangnyangbot.config.oauth;

import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.config.property.ChzzkProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
@RequiredArgsConstructor
public class ChzzkOAuth2ClientRegistrationConfig {

    public static final String REGISTRATION_ID = "chzzk";
    private static final String AUTHORIZATION_URI = "https://chzzk.naver.com/account-interlock";
    private static final String TOKEN_URI = "https://openapi.chzzk.naver.com/auth/v1/token";
    private static final String USER_INFO_URI = "https://openapi.chzzk.naver.com/open/v1/users/me";

    private final ChzzkProperty chzzkProperty;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(chzzkClientRegistration());
    }

    @Bean
    public ChzzkOAuth2AuthorizationRequestResolver chzzkOAuth2AuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository
    ) {
        return new ChzzkOAuth2AuthorizationRequestResolver(clientRegistrationRepository);
    }

    private ClientRegistration chzzkClientRegistration() {
        return ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .clientId(chzzkProperty.clientId())
                .clientSecret(chzzkProperty.clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(chzzkProperty.redirectUri())
                .authorizationUri(AUTHORIZATION_URI)
                .tokenUri(TOKEN_URI)
                .userInfoUri(USER_INFO_URI)
                .userNameAttributeName(ChzzkOAuth2UserService.CHANNEL_ID_ATTRIBUTE)
                .clientName("Chzzk")
                .build();
    }
}
