package org.nowstart.nyangnyangbot.application.service.overlay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayTokenPort;

class OverlayTokenServiceTest {

    @Test
    void issueRevokesAndFlushesBeforeInsertingNewActiveToken() {
        OverlayTokenPort port = Mockito.mock(OverlayTokenPort.class);
        Instant issuedAt = Instant.parse("2026-07-23T00:00:00Z");
        OverlayTokenService service = new OverlayTokenService(port) {
            @Override
            Instant now() {
                return issuedAt;
            }
        };
        given(port.saveIssuedToken(anyString(), Mockito.eq("admin-1"), Mockito.eq(issuedAt)))
                .willReturn(7L);

        var result = service.issueToken("admin-1");

        InOrder order = inOrder(port);
        order.verify(port).revokeActiveAndFlush(issuedAt);
        order.verify(port).saveIssuedToken(service.hashToken(result.token()), "admin-1", issuedAt);
    }

    @Test
    void validateHashesRawTokenAndOnlyAcceptsActiveHash() {
        OverlayTokenPort port = Mockito.mock(OverlayTokenPort.class);
        OverlayTokenService service = new OverlayTokenService(port);
        String token = "raw-token";
        given(port.existsActiveTokenHash(service.hashToken(token))).willReturn(true);

        assertThat(service.validateToken(token)).isTrue();
        assertThat(service.validateToken(" ")).isFalse();
    }
}
