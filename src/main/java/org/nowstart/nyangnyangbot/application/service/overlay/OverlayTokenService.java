package org.nowstart.nyangnyangbot.application.service.overlay;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.overlay.IssueOverlayTokenUseCase;
import org.nowstart.nyangnyangbot.application.port.in.overlay.IssueOverlayTokenUseCase.OverlayTokenIssueResult;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayTokenPort;
import org.nowstart.nyangnyangbot.domain.overlay.OverlayTokenPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OverlayTokenService implements IssueOverlayTokenUseCase {

    private final OverlayTokenPolicy overlayTokenPolicy = new OverlayTokenPolicy();
    private final OverlayTokenPort overlayTokenPort;

    @Override
    @Transactional
    public OverlayTokenIssueResult issueToken(String actorId) {
        LocalDateTime now = LocalDateTime.now();
        overlayTokenPort.revokeActive(now);
        String rawToken = overlayTokenPolicy.generateToken();
        Long tokenId = overlayTokenPort.saveIssuedToken(hashToken(rawToken), actorId);
        log.info("level=AUDIT action=overlay_token.rotate result=success actor={} tokenId={}", actorId, tokenId);
        return new OverlayTokenIssueResult(tokenId, rawToken);
    }

    @Transactional(readOnly = true)
    public boolean validateToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        return overlayTokenPort.existsActiveTokenHash(hashToken(rawToken));
    }

    String hashToken(String rawToken) {
        return overlayTokenPolicy.hashToken(rawToken);
    }
}
