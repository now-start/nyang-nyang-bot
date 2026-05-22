package org.nowstart.nyangnyangbot.adapter.in.web.overlay;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.in.web.overlay.response.OverlayTokenIssueResponse;
import org.nowstart.nyangnyangbot.application.port.in.overlay.IssueOverlayTokenUseCase.OverlayTokenIssueResult;
import org.nowstart.nyangnyangbot.application.service.overlay.OverlayDisplayService;
import org.nowstart.nyangnyangbot.application.service.overlay.OverlayTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AdminOverlayControllerTest {

    @Mock
    private OverlayTokenService overlayTokenService;

    @Mock
    private OverlayDisplayService overlayDisplayService;

    @Test
    @DisplayName("토큰 발급 시 인증된 관리자 ID를 actor로 사용한다")
    void issueToken_ShouldUseAuthenticatedAdminAsActor() {
        // 준비
        AdminOverlayController controller = new AdminOverlayController(overlayTokenService, overlayDisplayService);
        OverlayTokenIssueResult result = new OverlayTokenIssueResult(1L, "raw-token");
        given(overlayTokenService.issueToken("admin-1")).willReturn(result);

        // 실행
        ResponseEntity<OverlayTokenIssueResponse> response = controller.issueToken(
                new UsernamePasswordAuthenticationToken("admin-1", "N/A")
        );

        // 검증
        then(response.getBody()).isEqualTo(OverlayTokenIssueResponse.from(result));
        BDDMockito.then(overlayTokenService).should().issueToken("admin-1");
    }
}
