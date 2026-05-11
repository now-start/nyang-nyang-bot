package org.nowstart.nyangnyangbot.application.service.overlay;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.overlay.dto.OverlayTokenIssueResult;
import org.nowstart.nyangnyangbot.application.port.out.overlay.repository.OverlayTokenPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OverlayTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OverlayTokenPort overlayTokenPort;

    @Transactional
    public OverlayTokenIssueResult issueToken(String actorId) {
        LocalDateTime now = LocalDateTime.now();
        overlayTokenPort.revokeActive(now);
        String rawToken = generateToken();
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
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
