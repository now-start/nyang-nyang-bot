package org.nowstart.nyangnyangbot.service;

import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChzzkOAuth2TokenService {

    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final AuthorizationService authorizationService;

    @Value("${chzzk.oauth.registration-id:chzzk}")
    private String registrationId;

    @Value("${chzzk.oauth.principal:chzzk}")
    private String principalName;

    public String getAccessTokenValue() {
        try {
            return authorizationService.getAccessToken().getAccessToken();
        } catch (RuntimeException ex) {
            // Fall back to Spring OAuth2 client when DB record is missing.
        }

        OAuth2AuthorizedClient storedClient =
                authorizedClientService.loadAuthorizedClient(registrationId, principalName);
        if (storedClient != null && storedClient.getAccessToken() != null) {
            return storedClient.getAccessToken().getTokenValue();
        }

        OAuth2AuthorizedClient client = authorizedClientManager.authorize(OAuth2AuthorizeRequest
                .withClientRegistrationId(registrationId)
                .principal(authentication())
                .build());

        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException("OAuth2 authorized client not available");
        }

        return client.getAccessToken().getTokenValue();
    }

    private Authentication authentication() {
        return new UsernamePasswordAuthenticationToken(principalName, "N/A", Collections.emptyList());
    }
}






