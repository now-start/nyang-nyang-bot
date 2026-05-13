package org.nowstart.nyangnyangbot.application.port.out.overlay;

import java.time.LocalDateTime;

public interface OverlayTokenPort {

    void revokeActive(LocalDateTime revokedAt);

    Long saveIssuedToken(String tokenHash, String actorId);

    boolean existsActiveTokenHash(String tokenHash);
}
