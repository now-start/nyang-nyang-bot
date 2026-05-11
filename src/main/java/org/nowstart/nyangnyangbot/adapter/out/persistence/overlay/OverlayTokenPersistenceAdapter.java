package org.nowstart.nyangnyangbot.adapter.out.persistence.overlay;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.out.overlay.repository.OverlayTokenPort;
import org.nowstart.nyangnyangbot.adapter.out.persistence.entity.OverlayTokenEntity;
import org.nowstart.nyangnyangbot.adapter.out.persistence.repository.OverlayTokenRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OverlayTokenPersistenceAdapter implements OverlayTokenPort {

    private final OverlayTokenRepository overlayTokenRepository;

    @Override
    public void revokeActive(LocalDateTime revokedAt) {
        overlayTokenRepository.findByActiveTrue().forEach(token -> token.revoke(revokedAt));
    }

    @Override
    public Long saveIssuedToken(String tokenHash, String actorId) {
        OverlayTokenEntity saved = overlayTokenRepository.save(OverlayTokenEntity.builder()
                .tokenHash(tokenHash)
                .active(true)
                .issuedBy(actorId)
                .build());
        return saved.getId();
    }

    @Override
    public boolean existsActiveTokenHash(String tokenHash) {
        return overlayTokenRepository.existsByTokenHashAndActiveTrue(tokenHash);
    }
}
