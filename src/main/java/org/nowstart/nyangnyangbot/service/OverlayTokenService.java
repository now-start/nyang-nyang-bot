package org.nowstart.nyangnyangbot.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.overlay.OverlayTokenDto;
import org.nowstart.nyangnyangbot.data.entity.OverlayTokenEntity;
import org.nowstart.nyangnyangbot.repository.OverlayTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OverlayTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OverlayTokenRepository overlayTokenRepository;

    @Transactional
    public OverlayTokenDto.IssueResponse issueToken(String actorId) {
        LocalDateTime now = LocalDateTime.now();
        overlayTokenRepository.findByActiveTrue().forEach(token -> token.revoke(now));
        String rawToken = generateToken();
        OverlayTokenEntity saved = overlayTokenRepository.save(OverlayTokenEntity.builder()
                .tokenHash(hashToken(rawToken))
                .active(true)
                .issuedBy(actorId)
                .build());
        log.info("level=AUDIT action=overlay_token.rotate result=success actor={} tokenId={}", actorId, saved.getId());
        return new OverlayTokenDto.IssueResponse(saved.getId(), rawToken);
    }

    @Transactional(readOnly = true)
    public boolean validateToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        return overlayTokenRepository.existsByTokenHashAndActiveTrue(hashToken(rawToken));
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
