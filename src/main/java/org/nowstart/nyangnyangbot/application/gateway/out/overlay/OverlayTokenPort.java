package org.nowstart.nyangnyangbot.application.gateway.out.overlay;

import java.time.LocalDateTime;

public interface OverlayTokenPort {

    void revokeActive(LocalDateTime revokedAt);

    Long saveIssuedToken(String tokenHash, String actorId);

    boolean existsActiveTokenHash(String tokenHash);
}
