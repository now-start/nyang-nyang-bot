package org.nowstart.nyangnyangbot.adapter.in.web;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.in.web.overlay.response.OverlayTokenIssueResponse;
import org.nowstart.nyangnyangbot.application.port.in.overlay.dto.OverlayTokenIssueResult;
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
    void issueToken_ShouldUseAuthenticatedAdminAsActor() {
        AdminOverlayController controller = new AdminOverlayController(overlayTokenService, overlayDisplayService);
        OverlayTokenIssueResult result = new OverlayTokenIssueResult(1L, "raw-token");
        given(overlayTokenService.issueToken("admin-1")).willReturn(result);

        ResponseEntity<OverlayTokenIssueResponse> response = controller.issueToken(
                new UsernamePasswordAuthenticationToken("admin-1", "N/A")
        );

        then(response.getBody()).isEqualTo(OverlayTokenIssueResponse.from(result));
        BDDMockito.then(overlayTokenService).should().issueToken("admin-1");
    }
}
