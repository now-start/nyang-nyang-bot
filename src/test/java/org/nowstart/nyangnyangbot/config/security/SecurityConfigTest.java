package org.nowstart.nyangnyangbot.config.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.repository.AuthorizationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

class SecurityConfigTest {

    @Test
    void securityFilterChainContextLoads() {
        assertThatCode(() -> {
            try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
                context.setServletContext(new MockServletContext());
                context.register(TestSecurityConfiguration.class);
                context.refresh();
            }
        }).doesNotThrowAnyException();
    }

    @Configuration
    @EnableWebSecurity
    @Import(SecurityConfig.class)
    static class TestSecurityConfiguration {

        @Bean
        AuthorizationRepository authorizationRepository() {
            return mock(AuthorizationRepository.class);
        }
    }
}
