package org.nowstart.nyangnyangbot.application.service.overlay;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.overlay.dto.OverlayDisplayDetail;
import org.nowstart.nyangnyangbot.application.port.out.overlay.repository.OverlayDisplayPort;
import org.nowstart.nyangnyangbot.domain.model.OverlayDisplayEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OverlayDisplayService {

    private static final int DEFAULT_DISPLAY_TTL_SECONDS = 120;
    private static final int DEFAULT_MAX_ANIMATED_ROUNDS = 5;

    private final OverlayTokenService overlayTokenService;
    private final OverlayDisplayPort overlayDisplayPort;

    @Transactional
    public void enqueue(Long rouletteEventId) {
        overlayDisplayPort.enqueueRouletteEvent(rouletteEventId, now().plusSeconds(DEFAULT_DISPLAY_TTL_SECONDS));
    }

    @Transactional
    public OverlayDisplayDetail replayRouletteEvent(Long rouletteEventId) {
        OverlayDisplayEvent saved = overlayDisplayPort.replayRouletteEvent(
                rouletteEventId,
                now().plusSeconds(DEFAULT_DISPLAY_TTL_SECONDS)
        );
        log.info("level=AUDIT action=overlay.replay result=success rouletteEventId={} displayEventId={}",
                rouletteEventId, saved.id());
        return toDetail(saved);
    }

    @Transactional
    public Optional<OverlayDisplayDetail> claimNextEvent(String authorizationHeader) {
        validateAuthorization(authorizationHeader);
        LocalDateTime current = now();
        overlayDisplayPort.markPendingExpiredBefore(current);
        return overlayDisplayPort.claimNextPending(current).map(this::toDetail);
    }

    @Transactional
    public void markDisplayed(Long displayEventId, String authorizationHeader) {
        validateAuthorization(authorizationHeader);
        overlayDisplayPort.markDisplayed(displayEventId, now());
    }

    OverlayDisplayDetail toDetail(OverlayDisplayEvent displayEvent) {
        return new OverlayDisplayDetail(
                displayEvent,
                displayEvent.rounds(),
                DEFAULT_MAX_ANIMATED_ROUNDS
        );
    }

    LocalDateTime now() {
        return LocalDateTime.now();
    }

    private void validateAuthorization(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (!overlayTokenService.validateToken(token)) {
            throw new IllegalArgumentException("invalid overlay token");
        }
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix)) {
            return null;
        }
        return authorizationHeader.substring(prefix.length()).trim();
    }
}
