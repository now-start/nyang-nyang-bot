package org.nowstart.nyangnyangbot.config.oauth;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.web.util.UriComponentsBuilder;

public class ChzzkOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String AUTHORIZATION_REQUEST_BASE_URI = "/oauth2/authorization";

    private final OAuth2AuthorizationRequestResolver delegate;

    public ChzzkOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                AUTHORIZATION_REQUEST_BASE_URI
        );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return customize(delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return customize(delegate.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest authorizationRequest) {
        if (authorizationRequest == null || !isChzzk(authorizationRequest)) {
            return authorizationRequest;
        }
        String authorizationRequestUri = UriComponentsBuilder
                .fromUriString(authorizationRequest.getAuthorizationUri())
                .replaceQueryParam("clientId", encode(authorizationRequest.getClientId()))
                .replaceQueryParam("redirectUri", encode(authorizationRequest.getRedirectUri()))
                .replaceQueryParam("state", encode(authorizationRequest.getState()))
                .build(true)
                .toUriString();

        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .authorizationRequestUri(authorizationRequestUri)
                .build();
    }

    private boolean isChzzk(OAuth2AuthorizationRequest authorizationRequest) {
        return Objects.equals(
                ChzzkOAuth2ClientRegistrationConfig.REGISTRATION_ID,
                authorizationRequest.getAttribute(OAuth2ParameterNames.REGISTRATION_ID)
        );
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
