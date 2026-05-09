package org.nowstart.nyangnyangbot.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.data.dto.overlay.OverlayDisplayDto;
import org.nowstart.nyangnyangbot.data.entity.OverlayDisplayEventEntity;
import org.nowstart.nyangnyangbot.data.entity.RouletteEventEntity;
import org.nowstart.nyangnyangbot.data.type.OverlayDisplayStatus;
import org.nowstart.nyangnyangbot.repository.OverlayDisplayEventRepository;
import org.nowstart.nyangnyangbot.repository.RouletteEventRepository;
import org.nowstart.nyangnyangbot.repository.RouletteRoundResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OverlayDisplayService {

    private static final int DEFAULT_DISPLAY_TTL_SECONDS = 120;
    private static final int DEFAULT_MAX_ANIMATED_ROUNDS = 5;

    private final OverlayTokenService overlayTokenService;
    private final OverlayDisplayEventRepository overlayDisplayEventRepository;
    private final RouletteEventRepository rouletteEventRepository;
    private final RouletteRoundResultRepository rouletteRoundResultRepository;

    @Transactional
    public void enqueue(Long rouletteEventId) {
        RouletteEventEntity rouletteEvent = rouletteEventRepository.findById(rouletteEventId)
                .orElseThrow(() -> new IllegalArgumentException("roulette event not found"));
        overlayDisplayEventRepository.save(OverlayDisplayEventEntity.builder()
                .rouletteEvent(rouletteEvent)
                .status(OverlayDisplayStatus.PENDING)
                .expiresAt(now().plusSeconds(DEFAULT_DISPLAY_TTL_SECONDS))
                .build());
    }

    @Transactional
    public OverlayDisplayDto.EventResponse replayRouletteEvent(Long rouletteEventId) {
        RouletteEventEntity rouletteEvent = rouletteEventRepository.findById(rouletteEventId)
                .orElseThrow(() -> new IllegalArgumentException("roulette event not found"));
        Long replayOf = overlayDisplayEventRepository.findByRouletteEventIdOrderByCreateDateDesc(rouletteEventId)
                .stream()
                .findFirst()
                .map(OverlayDisplayEventEntity::getId)
                .orElse(null);
        OverlayDisplayEventEntity saved = overlayDisplayEventRepository.save(OverlayDisplayEventEntity.builder()
                .rouletteEvent(rouletteEvent)
                .replayOfDisplayEventId(replayOf)
                .status(OverlayDisplayStatus.PENDING)
                .expiresAt(now().plusSeconds(DEFAULT_DISPLAY_TTL_SECONDS))
                .build());
        return toResponse(saved);
    }

    @Transactional
    public Optional<OverlayDisplayDto.EventResponse> claimNextEvent(String authorizationHeader) {
        validateAuthorization(authorizationHeader);
        LocalDateTime current = now();
        overlayDisplayEventRepository.findByStatusAndExpiresAtBefore(OverlayDisplayStatus.PENDING, current)
                .forEach(OverlayDisplayEventEntity::markMissed);
        return overlayDisplayEventRepository.findFirstByStatusAndExpiresAtAfterOrderByCreateDateAsc(
                        OverlayDisplayStatus.PENDING,
                        current
                )
                .map(displayEvent -> {
                    displayEvent.markDisplaying(current);
                    return toResponse(displayEvent);
                });
    }

    @Transactional
    public void markDisplayed(Long displayEventId, String authorizationHeader) {
        validateAuthorization(authorizationHeader);
        OverlayDisplayEventEntity displayEvent = overlayDisplayEventRepository.findById(displayEventId)
                .orElseThrow(() -> new IllegalArgumentException("overlay display event not found"));
        displayEvent.markDisplayed(now());
    }

    OverlayDisplayDto.EventResponse toResponse(OverlayDisplayEventEntity displayEvent) {
        return OverlayDisplayDto.EventResponse.from(
                displayEvent,
                rouletteRoundResultRepository.findByRouletteEventIdOrderByRoundNoAsc(
                        displayEvent.getRouletteEvent().getId()
                ),
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
