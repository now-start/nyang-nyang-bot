package org.nowstart.nyangnyangbot.application.port.out.overlay;

import java.time.Instant;

public interface OverlayTokenPort {

    void revokeActiveAndFlush(Instant revokedAt);

    Long saveIssuedToken(String tokenHash, String actorUserId, Instant issuedAt);

    boolean existsActiveTokenHash(String tokenHash);
}
