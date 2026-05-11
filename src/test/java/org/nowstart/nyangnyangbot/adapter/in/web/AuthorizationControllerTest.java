package org.nowstart.nyangnyangbot.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.never;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.service.authorization.OAuthStateService;
import org.nowstart.nyangnyangbot.domain.model.AuthorizationAccount;
import org.nowstart.nyangnyangbot.config.property.ChzzkProperty;
import org.nowstart.nyangnyangbot.application.service.authorization.AuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthorizationControllerTest {

    @Mock
    private ChzzkProperty chzzkProperty;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private OAuthStateService oAuthStateService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void login_ShouldStoreGeneratedStateAndRedirectWithState() {
        MockHttpSession session = new MockHttpSession();
        AuthorizationController controller = newController();

        given(chzzkProperty.clientId()).willReturn("client-id");
        given(chzzkProperty.redirectUri()).willReturn("http://localhost:8080");
        given(oAuthStateService.generateState()).willReturn("state-value");

        String result = controller.login(session);

        String encodedRedirectUri = URLEncoder.encode("http://localhost:8080/token", StandardCharsets.UTF_8);
        assertThat(result).isEqualTo("redirect:https://chzzk.naver.com/account-interlock?"
                + "clientId=client-id"
                + "&redirectUri=" + encodedRedirectUri
                + "&state=state-value");
        assertThat(session.getAttribute(AuthorizationController.OAUTH_STATE_SESSION_ATTRIBUTE))
                .isEqualTo("state-value");
    }

    @Test
    void token_ShouldRejectCallbackWhenStateDoesNotMatch() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthorizationController.OAUTH_STATE_SESSION_ATTRIBUTE, "expected-state");
        AuthorizationController controller = newController();

        given(oAuthStateService.matches("expected-state", "wrong-state")).willReturn(false);

        assertThatThrownBy(() -> controller.token("code", "wrong-state", session))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        assertThat(session.getAttribute(AuthorizationController.OAUTH_STATE_SESSION_ATTRIBUTE)).isNull();
        then(authorizationService).should(never()).getAccessToken(anyString(), anyString());
    }

    @Test
    void token_ShouldAuthenticateAndRedirectWhenStateMatches() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthorizationController.OAUTH_STATE_SESSION_ATTRIBUTE, "expected-state");
        AuthorizationController controller = newController();
        AuthorizationAccount authorization = new AuthorizationAccount(
                "channel-1",
                "tester",
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                null
        );

        given(oAuthStateService.matches("expected-state", "expected-state")).willReturn(true);
        given(authorizationService.getAccessToken("code", "expected-state")).willReturn(authorization);
        given(chzzkProperty.redirectUri()).willReturn("http://localhost:8080");

        String result = controller.token("code", "expected-state", session);

        SecurityContext storedContext = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );
        assertThat(result).isEqualTo("redirect:http://localhost:8080/favorite/list");
        assertThat(session.getAttribute(AuthorizationController.OAUTH_STATE_SESSION_ATTRIBUTE)).isNull();
        assertThat(storedContext.getAuthentication().getName()).isEqualTo("channel-1");
        assertThat(storedContext.getAuthentication().getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    private AuthorizationController newController() {
        return new AuthorizationController(chzzkProperty, authorizationService, oAuthStateService);
    }
}
