package org.nowstart.nyangnyangbot.config;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

class OAuth2ClientConfigTest {

    @Test
    void authorizedClientManager_ShouldCreateManager() {
        OAuth2ClientConfig config = new OAuth2ClientConfig();
        ClientRegistrationRepository registrations = Mockito.mock(ClientRegistrationRepository.class);
        OAuth2AuthorizedClientService clientService = Mockito.mock(OAuth2AuthorizedClientService.class);

        OAuth2AuthorizedClientManager manager = config.authorizedClientManager(registrations, clientService);

        then(manager).isNotNull();
    }

    @Test
    void restOperations_ShouldCreateRestTemplate() {
        OAuth2ClientConfig config = new OAuth2ClientConfig();

        then(config.restOperations()).isNotNull();
    }
}






