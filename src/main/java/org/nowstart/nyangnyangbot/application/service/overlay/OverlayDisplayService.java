package org.nowstart.nyangnyangbot.application.service.overlay;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.overlay.ManageOverlayDisplayUseCase;
import org.nowstart.nyangnyangbot.application.port.in.overlay.ManageOverlayDisplayUseCase.OverlayDisplayResult;
import org.nowstart.nyangnyangbot.application.port.in.overlay.QueueOverlayDisplayUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteRoundResult;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayDisplayPort;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayDisplayPort.DisplayEventResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OverlayDisplayService implements ManageOverlayDisplayUseCase, QueueOverlayDisplayUseCase {

    private static final int DEFAULT_DISPLAY_TTL_SECONDS = 120;
    private static final int DEFAULT_MAX_ANIMATED_ROUNDS = 5;

    private final OverlayTokenService overlayTokenService;
    private final OverlayDisplayPort overlayDisplayPort;

    @Override
    @Transactional
    public void enqueueRouletteEvent(Long rouletteEventId) {
        overlayDisplayPort.enqueueRouletteEvent(rouletteEventId, now().plusSeconds(DEFAULT_DISPLAY_TTL_SECONDS));
    }

    @Override
    @Transactional
    public OverlayDisplayResult replayRouletteEvent(Long rouletteEventId) {
        DisplayEventResult saved = overlayDisplayPort.replayRouletteEvent(
                rouletteEventId,
                now().plusSeconds(DEFAULT_DISPLAY_TTL_SECONDS)
        );
        log.info("level=AUDIT action=overlay.replay result=success rouletteEventId={} displayEventId={}",
                rouletteEventId, saved.id());
        return overlayDisplayResult(saved);
    }

    @Override
    @Transactional
    public Optional<OverlayDisplayResult> claimNextEvent(String authorizationHeader) {
        validateAuthorization(authorizationHeader);
        LocalDateTime current = now();
        overlayDisplayPort.markPendingExpiredBefore(current);
        return overlayDisplayPort.claimNextPending(current).map(this::overlayDisplayResult);
    }

    @Override
    @Transactional
    public void markDisplayed(Long displayEventId, String authorizationHeader) {
        validateAuthorization(authorizationHeader);
        overlayDisplayPort.markDisplayed(displayEventId, now());
    }

    OverlayDisplayResult overlayDisplayResult(DisplayEventResult displayEvent) {
        return new OverlayDisplayResult(
                displayEvent.id(),
                displayEvent.rouletteEventId(),
                displayEvent.nickName(),
                displayEvent.roundCount(),
                DEFAULT_MAX_ANIMATED_ROUNDS,
                displayEvent.expiresAt(),
                displayEvent.rounds().stream().map(this::rouletteRoundResult).toList()
        );
    }

    private RouletteRoundResult rouletteRoundResult(RoundResult round) {
        return new RouletteRoundResult(
                round.id(),
                round.roundNo(),
                round.itemLabel(),
                round.losingItem(),
                round.rewardType() == null ? null : round.rewardType().name(),
                round.conversionMode() == null ? null : round.conversionMode().name(),
                round.exchangeFavoriteValue(),
                round.status() == null ? null : round.status().name(),
                round.ledgerId(),
                round.userUpboId(),
                round.failureReason()
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
