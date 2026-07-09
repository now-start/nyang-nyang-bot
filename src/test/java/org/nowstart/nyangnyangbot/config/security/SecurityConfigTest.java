package org.nowstart.nyangnyangbot.config.security;

import static org.assertj.core.api.BDDAssertions.thenCode;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.nowstart.nyangnyangbot.adapter.in.web.google.GoogleController;
import org.nowstart.nyangnyangbot.adapter.in.web.root.RootController;
import org.nowstart.nyangnyangbot.application.port.in.google.SyncGoogleSheetUseCase;
import org.nowstart.nyangnyangbot.config.SecurityConfig;
import org.nowstart.nyangnyangbot.config.oauth.ChzzkOAuth2AccessTokenResponseClient;
import org.nowstart.nyangnyangbot.config.oauth.ChzzkOAuth2AuthorizationRequestResolver;
import org.nowstart.nyangnyangbot.config.oauth.ChzzkOAuth2UserService;
import org.springframework.boot.test.util.TestPropertyValues;
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
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

class SecurityConfigTest {

    @Test
    void securityFilterChainContextLoads() {
        // 실행 및 검증
        thenCode(() -> {
            try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
                context.setServletContext(new MockServletContext());
                context.register(TestSecurityConfiguration.class);
                context.refresh();
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void adminEndpointRejectsAuthenticatedUserWithoutAdminRole() throws Exception {
        // 준비
        try (AnnotationConfigWebApplicationContext context = createWebContext()) {
            MockMvc mockMvc = createMockMvc(context);

        // 실행
            mockMvc.perform(post("/google/sync")
                            .session(session("user", Collections.emptyList()))
                            .with(csrf()))
        // 검증
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void unauthenticatedProtectedPathRedirectsToChzzkOAuth2AuthorizationEndpoint() throws Exception {
        // 준비
        try (AnnotationConfigWebApplicationContext context = createWebContext()) {
            MockMvc mockMvc = createMockMvc(context);

        // 실행 및 검증
            mockMvc.perform(get("/favorite/list"))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", "/oauth2/authorization/chzzk"));
        }
    }

    @Test
    void unauthenticatedRootRendersLandingWithoutRedirect() throws Exception {
        // 준비
        try (AnnotationConfigWebApplicationContext context = createWebContext()) {
            MockMvc mockMvc = createMockMvc(context);

        // 실행 및 검증 — "/"는 익명 접근 허용, 랜딩 뷰 렌더
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void authenticatedRootRedirectsToFavoriteList() throws Exception {
        // 준비
        try (AnnotationConfigWebApplicationContext context = createWebContext()) {
            MockMvc mockMvc = createMockMvc(context);

        // 실행 및 검증
            mockMvc.perform(get("/").session(session("user", Collections.emptyList())))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", "/favorite/list"));
        }
    }

    @Test
    void adminEndpointAllowsUserWithAdminRole() throws Exception {
        // 준비
        try (AnnotationConfigWebApplicationContext context = createWebContext()) {
            MockMvc mockMvc = createMockMvc(context);
            SyncGoogleSheetUseCase syncGoogleSheetUseCase = context.getBean(SyncGoogleSheetUseCase.class);

        // 실행
            mockMvc.perform(post("/google/sync")
                            .session(session(
                                    "admin",
                                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                            ))
                            .with(csrf()))
        // 검증
                    .andExpect(status().isOk());

            BDDMockito.then(syncGoogleSheetUseCase).should().updateFavorite();
        }
    }

    @Test
    void adminSyncRejectsMissingCsrfToken() throws Exception {
        // 준비
        try (AnnotationConfigWebApplicationContext context = createWebContext()) {
            MockMvc mockMvc = createMockMvc(context);

        // 실행 및 검증
            mockMvc.perform(post("/google/sync")
                            .session(session(
                                    "admin",
                                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                            )))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void localAuthAllowsAdminEndpointWithoutSessionWhenEnabled() throws Exception {
        // 준비
        try (AnnotationConfigWebApplicationContext context = createWebContext(
                "nyang.local-auth.enabled=true",
                "nyang.local-auth.user-id=local-channel",
                "nyang.local-auth.admin=true"
        )) {
            MockMvc mockMvc = createMockMvc(context);
            SyncGoogleSheetUseCase syncGoogleSheetUseCase = context.getBean(SyncGoogleSheetUseCase.class);

        // 실행
            mockMvc.perform(post("/google/sync").with(csrf()))
        // 검증
                    .andExpect(status().isOk());

            BDDMockito.then(syncGoogleSheetUseCase).should().updateFavorite();
        }
    }

    @Test
    void h2ConsoleAllowsSameOriginFrameWhenEnabled() throws Exception {
        // 준비
        try (AnnotationConfigWebApplicationContext context = createWebContext(
                "spring.h2.console.enabled=true"
        )) {
            MockMvc mockMvc = createMockMvc(context);

        // 실행 및 검증
            mockMvc.perform(get("/h2-console/login.jsp"))
                    .andExpect(status().isNotFound())
                    .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
        }
    }

    @Test
    void authenticatedPostRejectsMissingCsrfToken() throws Exception {
        // 준비
        try (AnnotationConfigWebApplicationContext context = createWebContext()) {
            MockMvc mockMvc = createMockMvc(context);

        // 실행 및 검증
            mockMvc.perform(post("/test/mutation").session(session(
                            "admin",
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                    )))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void authenticatedPostAllowsValidCsrfToken() throws Exception {
        // 준비
        try (AnnotationConfigWebApplicationContext context = createWebContext()) {
            MockMvc mockMvc = createMockMvc(context);

        // 실행 및 검증
            mockMvc.perform(post("/test/mutation")
                            .session(session(
                                    "admin",
                                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                            ))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("MUTATED"));
        }
    }

    @Test
    void overlayPollingEndpointAllowsGetWithoutAuthentication() throws Exception {
        // 준비
        try (AnnotationConfigWebApplicationContext context = createWebContext()) {
            MockMvc mockMvc = createMockMvc(context);

        // 실행 및 검증
            mockMvc.perform(get("/overlay/roulette/events/next"))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void otherOverlayEventPostRejectsMissingCsrfToken() throws Exception {
        // 준비
        try (AnnotationConfigWebApplicationContext context = createWebContext()) {
            MockMvc mockMvc = createMockMvc(context);

        // 실행 및 검증
            mockMvc.perform(post("/overlay/roulette/events/1/replay"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void h2ConsolePostAllowsMissingCsrfTokenWhenEnabled() throws Exception {
        // 준비
        try (AnnotationConfigWebApplicationContext context = createWebContext(
                "spring.h2.console.enabled=true"
        )) {
            MockMvc mockMvc = createMockMvc(context);

        // 실행 및 검증
            mockMvc.perform(post("/h2-console/login.do"))
                    .andExpect(status().isNotFound());
        }
    }

    private AnnotationConfigWebApplicationContext createWebContext(String... properties) {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        TestPropertyValues.of(properties).applyTo(context);
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
        SyncGoogleSheetUseCase syncGoogleSheetUseCase() {
            return mock(SyncGoogleSheetUseCase.class);
        }

        @Bean
        GoogleController googleController(SyncGoogleSheetUseCase syncGoogleSheetUseCase) {
            return new GoogleController(syncGoogleSheetUseCase);
        }

        @Bean
        TestMutationController testMutationController() {
            return new TestMutationController();
        }

        @Bean
        RootController rootController() {
            return new RootController();
        }

        @Bean
        ViewResolver viewResolver() {
            return (viewName, locale) -> {
                if (viewName.startsWith("redirect:")) {
                    return new RedirectView(viewName.substring("redirect:".length()));
                }
                return (model, request, response) -> {
                };
            };
        }

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("chzzk")
                    .clientId("client-id")
                    .clientSecret("client-secret")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("http://localhost/login/oauth2/code/chzzk")
                    .authorizationUri("https://chzzk.naver.com/account-interlock")
                    .tokenUri("https://openapi.chzzk.naver.com/auth/v1/token")
                    .userInfoUri("https://openapi.chzzk.naver.com/open/v1/users/me")
                    .userNameAttributeName("channelId")
                    .clientName("Chzzk")
                    .build());
        }

        @Bean
        ChzzkOAuth2AuthorizationRequestResolver chzzkOAuth2AuthorizationRequestResolver() {
            return mock(ChzzkOAuth2AuthorizationRequestResolver.class);
        }

        @Bean
        ChzzkOAuth2AccessTokenResponseClient chzzkOAuth2AccessTokenResponseClient() {
            return mock(ChzzkOAuth2AccessTokenResponseClient.class);
        }

        @Bean
        ChzzkOAuth2UserService chzzkOAuth2UserService() {
            return mock(ChzzkOAuth2UserService.class);
        }
    }

    @RestController
    static class TestMutationController {

        @PostMapping("/test/mutation")
        String mutate() {
            return "MUTATED";
        }

    }
}
