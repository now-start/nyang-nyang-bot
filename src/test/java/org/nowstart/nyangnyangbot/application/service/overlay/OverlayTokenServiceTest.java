package org.nowstart.nyangnyangbot.application.service.overlay;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import org.mockito.BDDMockito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.overlay.IssueOverlayTokenUseCase.OverlayTokenIssueResult;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayTokenPort;

@ExtendWith(MockitoExtension.class)
class OverlayTokenServiceTest {

    @Mock
    private OverlayTokenPort overlayTokenPort;

    @Test
    void issueToken_ShouldStoreOnlyTokenHashAndRevokeActiveTokens() {
        // 준비
        given(overlayTokenPort.saveIssuedToken(any(), any())).willReturn(2L);
        OverlayTokenService service = new OverlayTokenService(overlayTokenPort);

        // 실행
        OverlayTokenIssueResult result = service.issueToken("admin-1");

        // 검증
        then(result.token()).isNotBlank();
        then(result.tokenId()).isEqualTo(2L);
        BDDMockito.then(overlayTokenPort).should().revokeActive(any());
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        BDDMockito.then(overlayTokenPort).should().saveIssuedToken(hashCaptor.capture(), argThat("admin-1"::equals));
        then(hashCaptor.getValue()).isNotEqualTo(result.token());
    }

    @Test
    void validateToken_ShouldCheckHashAgainstActiveToken() {
        // 실행
        OverlayTokenService service = new OverlayTokenService(overlayTokenPort);
        String hash = service.hashToken("raw-token");
        given(overlayTokenPort.existsActiveTokenHash(hash)).willReturn(true);

        // 검증
        then(service.validateToken("raw-token")).isTrue();

        BDDMockito.then(overlayTokenPort).should().existsActiveTokenHash(argThat(hash::equals));
    }
}
