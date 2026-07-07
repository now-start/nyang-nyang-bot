package org.nowstart.nyangnyangbot.adapter.in.web.overlay;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.overlay.IssueOverlayTokenUseCase.OverlayTokenIssueResult;
import org.nowstart.nyangnyangbot.application.service.overlay.OverlayDisplayService;
import org.nowstart.nyangnyangbot.application.service.overlay.OverlayTokenService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.ui.ConcurrentModel;

@ExtendWith(MockitoExtension.class)
class AdminOverlayControllerTest {

    @Mock
    private OverlayTokenService overlayTokenService;

    @Mock
    private OverlayDisplayService overlayDisplayService;

    @Test
    @DisplayName("토큰 발급 시 인증된 관리자 ID를 actor로 사용하고 fragment token URL을 반환한다")
    void issueToken_ShouldUseAuthenticatedAdminAsActorAndReturnFragment() {
        // 준비
        AdminOverlayController controller = new AdminOverlayController(overlayTokenService, overlayDisplayService);
        OverlayTokenIssueResult result = new OverlayTokenIssueResult(1L, "raw-token");
        given(overlayTokenService.issueToken("admin-1")).willReturn(result);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/overlay/roulette/token");
        request.setScheme("https");
        request.setServerName("example.com");
        request.setServerPort(443);
        ConcurrentModel model = new ConcurrentModel();

        // 실행
        String view = controller.issueToken(
                new UsernamePasswordAuthenticationToken("admin-1", "N/A"),
                request,
                model
        );

        // 검증
        then(view).isEqualTo("features/roulette/components :: overlay-token-url-field");
        then(model.getAttribute("tokenUrl")).isEqualTo("https://example.com/overlay/roulette#token=raw-token");
        BDDMockito.then(overlayTokenService).should().issueToken("admin-1");
    }

    @Test
    @DisplayName("재송출 요청은 feedback fragment를 반환한다")
    void replay_ShouldReturnFeedbackFragment() {
        // 준비
        AdminOverlayController controller = new AdminOverlayController(overlayTokenService, overlayDisplayService);
        ConcurrentModel model = new ConcurrentModel();

        // 실행
        String view = controller.replay(10L, model);

        // 검증
        then(view).isEqualTo("components/feedback :: alert");
        then(model.getAttribute("tone")).isEqualTo("success");
        BDDMockito.then(overlayDisplayService).should().replayRouletteEvent(10L);
    }
}
