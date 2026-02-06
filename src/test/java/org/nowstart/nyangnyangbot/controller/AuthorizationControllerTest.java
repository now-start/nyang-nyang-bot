package org.nowstart.nyangnyangbot.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.data.entity.AuthorizationEntity;
import org.nowstart.nyangnyangbot.data.property.ChzzkProperty;
import org.nowstart.nyangnyangbot.repository.AuthorizationRepository;
import org.nowstart.nyangnyangbot.service.AuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthorizationController.class)
class AuthorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChzzkProperty chzzkProperty;

    @MockitoBean
    private AuthorizationService authorizationService;

    @MockitoBean
    private AuthorizationRepository authorizationRepository;

    @Nested
    @DisplayName("로그인 요청 시")
    class LoginTests {

        @Test
        @DisplayName("치지직 로그인 페이지로 리다이렉트한다")
        void redirectToChzzkLoginPage() throws Exception {
            // given
            given(chzzkProperty.getClientId()).willReturn("testClientId");
            given(chzzkProperty.getRedirectUri()).willReturn("http://localhost:8080");

            // when & then
            mockMvc.perform(get("/authorization/login"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("https://chzzk.naver.com/account-interlock?*clientId=testClientId*"));

            then(chzzkProperty).should().getClientId();
            then(chzzkProperty).should().getRedirectUri();
        }

        @Test
        @DisplayName("redirectUri를 URL 인코딩하여 전달한다")
        void encodeRedirectUri() throws Exception {
            // given
            given(chzzkProperty.getClientId()).willReturn("testClientId");
            given(chzzkProperty.getRedirectUri()).willReturn("http://localhost:8080");

            // when & then
            mockMvc.perform(get("/authorization/login"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("*redirectUri=http%3A%2F%2Flocalhost%3A8080%2Fauthorization%2Ftoken*"));
        }

        @Test
        @DisplayName("고정된 state 값을 포함한다")
        void includeFixedState() throws Exception {
            // given
            given(chzzkProperty.getClientId()).willReturn("testClientId");
            given(chzzkProperty.getRedirectUri()).willReturn("http://localhost:8080");

            // when & then
            mockMvc.perform(get("/authorization/login"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("*state=zxclDasdfA25*"));
        }

        @Test
        @DisplayName("프로퍼티에서 제공된 clientId를 사용한다")
        void useClientIdFromProperty() throws Exception {
            // given
            given(chzzkProperty.getClientId()).willReturn("customClientId123");
            given(chzzkProperty.getRedirectUri()).willReturn("http://localhost:8080");

            // when & then
            mockMvc.perform(get("/authorization/login"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("*clientId=customClientId123*"));
        }

        @Test
        @DisplayName("올바른 URL 구조를 생성한다")
        void buildCorrectUrl() throws Exception {
            // given
            given(chzzkProperty.getClientId()).willReturn("testClientId");
            given(chzzkProperty.getRedirectUri()).willReturn("http://localhost:8080");

            // when & then
            mockMvc.perform(get("/authorization/login"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("https://chzzk.naver.com/account-interlock?*clientId=*&redirectUri=*&state=*"));
        }

        @Test
        @DisplayName("다른 redirectUri를 인코딩하여 전달한다")
        void encodeCustomRedirectUri() throws Exception {
            // given
            given(chzzkProperty.getClientId()).willReturn("testClientId");
            given(chzzkProperty.getRedirectUri()).willReturn("https://example.com:9000");

            // when & then
            mockMvc.perform(get("/authorization/login"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("*https%3A%2F%2Fexample.com%3A9000%2Fauthorization%2Ftoken*"));
        }
    }

    @Nested
    @DisplayName("토큰 요청 시")
    class TokenTests {

        @Test
        @DisplayName("AuthorizationService를 호출하고 즐겨찾기 목록으로 리다이렉트한다")
        void callServiceAndRedirect() throws Exception {
            // given
            String code = "authCode123";
            String state = "zxclDasdfA25";
            given(chzzkProperty.getRedirectUri()).willReturn("http://localhost:8080");
            given(authorizationService.getAccessToken(code, state)).willReturn(
                    AuthorizationEntity.builder().channelId("channel123").admin(true).build()
            );

            // when & then
            mockMvc.perform(get("/authorization/token")
                            .param("code", code)
                            .param("state", state))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("http://localhost:8080/favorite/list"));

            then(authorizationService).should().getAccessToken(code, state);
            then(chzzkProperty).should().getRedirectUri();
        }

        @Test
        @DisplayName("다른 code 값들을 처리한다")
        void handleDifferentCodes() throws Exception {
            // given
            String code1 = "code123";
            String code2 = "differentCode456";
            String state = "state123";
            given(chzzkProperty.getRedirectUri()).willReturn("http://localhost:8080");
            given(authorizationService.getAccessToken(anyString(), anyString())).willReturn(
                    AuthorizationEntity.builder().channelId("channel123").admin(true).build()
            );

            // when & then
            mockMvc.perform(get("/authorization/token")
                            .param("code", code1)
                            .param("state", state))
                    .andExpect(status().is3xxRedirection());

            mockMvc.perform(get("/authorization/token")
                            .param("code", code2)
                            .param("state", state))
                    .andExpect(status().is3xxRedirection());

            then(authorizationService).should().getAccessToken(code1, state);
            then(authorizationService).should().getAccessToken(code2, state);
        }

        @Test
        @DisplayName("즐겨찾기 목록 페이지로 리다이렉트한다")
        void redirectToFavoriteList() throws Exception {
            // given
            given(chzzkProperty.getRedirectUri()).willReturn("http://localhost:8080");
            given(authorizationService.getAccessToken(anyString(), anyString())).willReturn(
                    AuthorizationEntity.builder().channelId("channel123").admin(true).build()
            );

            // when & then
            mockMvc.perform(get("/authorization/token")
                            .param("code", "code")
                            .param("state", "state"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("*/favorite/list"));
        }

        @Test
        @DisplayName("빈 code와 state를 처리한다")
        void handleEmptyCodeAndState() throws Exception {
            // given
            String emptyCode = "";
            String emptyState = "";
            given(chzzkProperty.getRedirectUri()).willReturn("http://localhost:8080");
            given(authorizationService.getAccessToken(emptyCode, emptyState)).willReturn(
                    AuthorizationEntity.builder().channelId("channel123").admin(true).build()
            );

            // when & then
            mockMvc.perform(get("/authorization/token")
                            .param("code", emptyCode)
                            .param("state", emptyState))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("http://localhost:8080/favorite/list"));

            then(authorizationService).should().getAccessToken(emptyCode, emptyState);
        }

        @Test
        @DisplayName("파라미터를 서비스에 전달한다")
        void passParametersToService() throws Exception {
            // given
            String code = "testCode123";
            String state = "testState456";
            given(chzzkProperty.getRedirectUri()).willReturn("http://localhost:8080");
            given(authorizationService.getAccessToken(code, state)).willReturn(
                    AuthorizationEntity.builder().channelId("channel123").admin(true).build()
            );

            // when & then
            mockMvc.perform(get("/authorization/token")
                            .param("code", code)
                            .param("state", state))
                    .andExpect(status().is3xxRedirection());

            then(authorizationService).should().getAccessToken(code, state);
        }
    }
}
