package org.nowstart.nyangnyangbot.controller;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.property.ChzzkProperty;
import org.nowstart.nyangnyangbot.service.AuthorizationService;

@ExtendWith(MockitoExtension.class)
class AuthorizationControllerTest {

    @Mock
    private ChzzkProperty chzzkProperty;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private AuthorizationController authorizationController;

    @BeforeEach
    void setUp() {
        given(chzzkProperty.getClientId()).willReturn("testClientId");
        given(chzzkProperty.getRedirectUri()).willReturn("http://localhost:8080");
    }

    @Test
    void login_ShouldRedirectToChzzkLoginPage() {
        // when
        String result = authorizationController.login();

        // then
        then(result).startsWith("redirect:https://chzzk.naver.com/account-interlock?");
        then(result).contains("clientId=testClientId");
        then(result).contains("state=zxclDasdfA25");
        BDDMockito.then(chzzkProperty).should().getClientId();
        BDDMockito.then(chzzkProperty).should().getRedirectUri();
    }

    @Test
    void login_ShouldEncodeRedirectUri() {
        // when
        String result = authorizationController.login();

        // then
        then(result).contains("redirectUri=http%3A%2F%2Flocalhost%3A8080%2Fauthorization%2Ftoken");
    }

    @Test
    void login_ShouldIncludeFixedState() {
        // when
        String result = authorizationController.login();

        // then
        then(result).contains("state=zxclDasdfA25");
    }

    @Test
    void token_ShouldCallAuthorizationServiceAndRedirect() {
        // given
        String code = "authCode123";
        String state = "zxclDasdfA25";

        // when
        String result = authorizationController.token(code, state);

        // then
        BDDMockito.then(authorizationService).should().getAccessToken(code, state);
        then(result).isEqualTo("redirect:http://localhost:8080/favorite/list");
        BDDMockito.then(chzzkProperty).should().getRedirectUri();
    }

    @Test
    void token_ShouldHandleDifferentCodes() {
        // given
        String code1 = "code123";
        String code2 = "differentCode456";
        String state = "state123";

        // when
        authorizationController.token(code1, state);
        authorizationController.token(code2, state);

        // then
        BDDMockito.then(authorizationService).should().getAccessToken(code1, state);
        BDDMockito.then(authorizationService).should().getAccessToken(code2, state);
    }

    @Test
    void token_ShouldRedirectToFavoriteList() {
        // given
        String code = "code";
        String state = "state";

        // when
        String result = authorizationController.token(code, state);

        // then
        then(result).contains("/favorite/list");
    }

    @Test
    void login_ShouldUseClientIdFromProperty() {
        // given
        given(chzzkProperty.getClientId()).willReturn("customClientId123");

        // when
        String result = authorizationController.login();

        // then
        then(result).contains("clientId=customClientId123");
    }

    @Test
    void login_ShouldBuildCorrectUrl() {
        // when
        String result = authorizationController.login();

        // then
        then(result).matches("redirect:https://chzzk\\.naver\\.com/account-interlock\\?.*");
        then(result).contains("clientId=");
        then(result).contains("redirectUri=");
        then(result).contains("state=");
    }

    @Test
    void token_ShouldHandleEmptyCodeAndState() {
        // given
        String emptyCode = "";
        String emptyState = "";

        // when
        String result = authorizationController.token(emptyCode, emptyState);

        // then
        BDDMockito.then(authorizationService).should().getAccessToken(emptyCode, emptyState);
        then(result).isEqualTo("redirect:http://localhost:8080/favorite/list");
    }

    @Test
    void token_ShouldPassParametersToService() {
        // given
        String code = "testCode123";
        String state = "testState456";

        // when
        authorizationController.token(code, state);

        // then
        BDDMockito.then(authorizationService).should().getAccessToken(code, state);
    }

    @Test
    void login_ShouldUseRedirectUriFromProperty() {
        // given
        given(chzzkProperty.getRedirectUri()).willReturn("https://example.com:9000");

        // when
        String result = authorizationController.login();

        // then
        then(result).contains("https%3A%2F%2Fexample.com%3A9000%2Fauthorization%2Ftoken");
    }
}
