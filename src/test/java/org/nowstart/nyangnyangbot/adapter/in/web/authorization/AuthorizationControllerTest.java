package org.nowstart.nyangnyangbot.adapter.in.web.authorization;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.authorization.LoginWithChzzkUseCase;
import org.nowstart.nyangnyangbot.application.service.authorization.AuthorizationService;
import org.nowstart.nyangnyangbot.application.service.authorization.OAuthStateService;
import org.nowstart.nyangnyangbot.config.property.ChzzkProperty;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;
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
    @DisplayName("로그인 시 생성된 state를 세션에 저장하고 state를 포함한 URL로 리다이렉트한다")
    void login_ShouldStoreGeneratedStateAndRedirectWithState() {
        // 준비
        MockHttpSession session = new MockHttpSession();
        AuthorizationController controller = newController();

        given(chzzkProperty.clientId()).willReturn("client-id");
        given(chzzkProperty.redirectUri()).willReturn("http://localhost:8080/token");
        given(oAuthStateService.generateState()).willReturn("state-value");

        String result = controller.login(session);

        // 실행
        String encodedRedirectUri = URLEncoder.encode("http://localhost:8080/token", StandardCharsets.UTF_8);
        // 검증
        then(result).isEqualTo("redirect:https://chzzk.naver.com/account-interlock?"
                + "clientId=client-id"
                + "&redirectUri=" + encodedRedirectUri
                + "&state=state-value");
        then(session.getAttribute(AuthorizationController.OAUTH_STATE_SESSION_ATTRIBUTE))
                .isEqualTo("state-value");
    }

    @Test
    @DisplayName("OAuth 로그인이 비활성화된 경우 즐겨찾기 목록으로 리다이렉트한다")
    void login_ShouldRedirectToFavoriteListWhenOAuthLoginIsDisabled() {
        // 준비
        MockHttpSession session = new MockHttpSession();
        AuthorizationController controller = newController();
        ReflectionTestUtils.setField(controller, "oauthLoginEnabled", false);

        // 실행
        String result = controller.login(session);

        // 검증
        then(result).isEqualTo("redirect:/favorite/list");
        then(session.getAttribute(AuthorizationController.OAUTH_STATE_SESSION_ATTRIBUTE)).isNull();
    }

    @Test
    @DisplayName("콜백 state가 일치하지 않으면 401 예외를 던지고 인증 서비스를 호출하지 않는다")
    void token_ShouldRejectCallbackWhenStateDoesNotMatch() {
        // 준비
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthorizationController.OAUTH_STATE_SESSION_ATTRIBUTE, "expected-state");
        AuthorizationController controller = newController();

        given(oAuthStateService.matches("expected-state", "wrong-state")).willReturn(false);

        // 실행 및 검증
        thenThrownBy(() -> controller.token("code", "wrong-state", session))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        then(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        then(session.getAttribute(AuthorizationController.OAUTH_STATE_SESSION_ATTRIBUTE)).isNull();
        BDDMockito.then(authorizationService).should(never()).login(anyString(), anyString());
    }

    @Test
    @DisplayName("state가 일치하면 인증하고 즐겨찾기 목록으로 리다이렉트하며 세션에 보안 컨텍스트를 저장한다")
    void token_ShouldAuthenticateAndRedirectWhenStateMatches() {
        // 준비
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthorizationController.OAUTH_STATE_SESSION_ATTRIBUTE, "expected-state");
        AuthorizationController controller = newController();
        LoginWithChzzkUseCase.Result authorization = new LoginWithChzzkUseCase.Result(
                "channel-1",
                true
        );

        given(oAuthStateService.matches("expected-state", "expected-state")).willReturn(true);
        given(authorizationService.login("code", "expected-state")).willReturn(authorization);

        String result = controller.token("code", "expected-state", session);

        // 실행
        SecurityContext storedContext = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );
        // 검증
        then(result).isEqualTo("redirect:/favorite/list");
        then(session.getAttribute(AuthorizationController.OAUTH_STATE_SESSION_ATTRIBUTE)).isNull();
        then(storedContext.getAuthentication().getName()).isEqualTo("channel-1");
        then(storedContext.getAuthentication().getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    private AuthorizationController newController() {
        return new AuthorizationController(chzzkProperty, authorizationService, oAuthStateService);
    }
}
