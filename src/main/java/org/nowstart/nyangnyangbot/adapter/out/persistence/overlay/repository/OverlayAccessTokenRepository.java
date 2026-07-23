package org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.repository;

import java.time.Instant;
import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.entity.OverlayAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OverlayAccessTokenRepository extends JpaRepository<OverlayAccessToken, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update OverlayAccessToken token set token.revokedAt = :revokedAt where token.revokedAt is null")
    int revokeActive(@Param("revokedAt") Instant revokedAt);

    boolean existsByTokenHashAndRevokedAtIsNull(String tokenHash);
}
