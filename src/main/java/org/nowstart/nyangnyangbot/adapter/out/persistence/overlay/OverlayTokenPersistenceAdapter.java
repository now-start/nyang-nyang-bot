package org.nowstart.nyangnyangbot.adapter.out.persistence.overlay;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.entity.OverlayToken;
import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.repository.OverlayTokenRepository;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayTokenPort;
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
        OverlayToken saved = overlayTokenRepository.save(OverlayToken.builder()
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
