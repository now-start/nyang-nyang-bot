package org.nowstart.nyangnyangbot.adapter.out.persistence.overlay;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.entity.OverlayAccessToken;
import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.repository.OverlayAccessTokenRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayTokenPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OverlayTokenPersistenceAdapter implements OverlayTokenPort {

    private final OverlayAccessTokenRepository overlayAccessTokenRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void revokeActiveAndFlush(Instant revokedAt) {
        overlayAccessTokenRepository.revokeActive(revokedAt);
        overlayAccessTokenRepository.flush();
    }

    @Override
    @Transactional
    public Long saveIssuedToken(String tokenHash, String actorUserId, Instant issuedAt) {
        UserAccount issuer = actorUserId == null || actorUserId.isBlank()
                ? null
                : entityManager.getReference(UserAccount.class, actorUserId);
        return overlayAccessTokenRepository.saveAndFlush(OverlayAccessToken.builder()
                .tokenHash(tokenHash)
                .issuedByUserAccount(issuer)
                .issuedAt(issuedAt)
                .build()).getId();
    }

    @Override
    public boolean existsActiveTokenHash(String tokenHash) {
        return overlayAccessTokenRepository.existsByTokenHashAndRevokedAtIsNull(tokenHash);
    }
}
