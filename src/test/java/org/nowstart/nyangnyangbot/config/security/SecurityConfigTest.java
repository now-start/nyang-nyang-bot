package org.nowstart.nyangnyangbot.config.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.config.SecurityConfig;
import org.nowstart.nyangnyangbot.controller.GoogleController;
import org.nowstart.nyangnyangbot.service.GoogleSheetService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
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

    @Test
    void adminEndpointRejectsAuthenticatedUserWithoutAdminRole() throws Exception {
        try (AnnotationConfigWebApplicationContext context = createWebContext()) {
            MockMvc mockMvc = createMockMvc(context);

            mockMvc.perform(get("/google/sync").session(session("user", Collections.emptyList())))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void adminEndpointAllowsUserWithAdminRole() throws Exception {
        try (AnnotationConfigWebApplicationContext context = createWebContext()) {
            MockMvc mockMvc = createMockMvc(context);
            GoogleSheetService googleSheetService = context.getBean(GoogleSheetService.class);

            mockMvc.perform(get("/google/sync").session(session(
                            "admin",
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                    )))
                    .andExpect(status().isOk())
                    .andExpect(content().string("SUCCESS"));

            then(googleSheetService).should().updateFavorite();
        }
    }

    private AnnotationConfigWebApplicationContext createWebContext() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestSecurityConfiguration.class);
        context.refresh();
        return context;
    }

    private MockMvc createMockMvc(AnnotationConfigWebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    private MockHttpSession session(String name, List<GrantedAuthority> authorities) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(name, "N/A", authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import(SecurityConfig.class)
    static class TestSecurityConfiguration {

        @Bean
        GoogleSheetService googleSheetService() {
            return mock(GoogleSheetService.class);
        }

        @Bean
        GoogleController googleController(GoogleSheetService googleSheetService) {
            return new GoogleController(googleSheetService);
        }
    }
}
