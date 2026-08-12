package org.nowstart.nyangnyangbot.application.port.out.overlay;

import java.time.Instant;

public interface OverlayTokenPort {

    /** 모든 활성 오버레이 토큰을 폐기하고 변경 사항을 즉시 반영한 뒤 반환한다. */
    void revokeActiveAndFlush(Instant revokedAt);

    /** 해시된 오버레이 토큰을 저장하고 식별자를 반환한다. */
    Long saveIssuedToken(String tokenHash, String actorUserId, Instant issuedAt);

    /** 토큰 해시가 폐기되지 않은 오버레이 토큰에 해당하는지 반환한다. */
    boolean existsActiveTokenHash(String tokenHash);
}
