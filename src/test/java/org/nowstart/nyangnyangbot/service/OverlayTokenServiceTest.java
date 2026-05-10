package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayTokenPort;
import org.nowstart.nyangnyangbot.data.dto.overlay.OverlayTokenDto;

@ExtendWith(MockitoExtension.class)
class OverlayTokenServiceTest {

    @Mock
    private OverlayTokenPort overlayTokenPort;

    @Test
    void issueToken_ShouldStoreOnlyTokenHashAndRevokeActiveTokens() {
        given(overlayTokenPort.saveIssuedToken(any(), any())).willReturn(2L);
        OverlayTokenService service = new OverlayTokenService(overlayTokenPort);

        OverlayTokenDto.IssueResponse response = service.issueToken("admin-1");

        assertThat(response.token()).isNotBlank();
        assertThat(response.tokenId()).isEqualTo(2L);
        then(overlayTokenPort).should().revokeActive(any());
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        then(overlayTokenPort).should().saveIssuedToken(hashCaptor.capture(), argThat("admin-1"::equals));
        assertThat(hashCaptor.getValue()).isNotEqualTo(response.token());
    }

    @Test
    void validateToken_ShouldCheckHashAgainstActiveToken() {
        OverlayTokenService service = new OverlayTokenService(overlayTokenPort);
        String hash = service.hashToken("raw-token");
        given(overlayTokenPort.existsActiveTokenHash(hash)).willReturn(true);

        assertThat(service.validateToken("raw-token")).isTrue();

        then(overlayTokenPort).should().existsActiveTokenHash(argThat(hash::equals));
    }
}
