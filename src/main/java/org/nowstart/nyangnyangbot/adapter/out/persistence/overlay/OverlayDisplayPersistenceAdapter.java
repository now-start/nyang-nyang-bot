package org.nowstart.nyangnyangbot.adapter.out.persistence.overlay;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.entity.OverlayDisplayEvent;
import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.repository.OverlayDisplayEventRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteEvent;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteRoundResult;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteEventRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRoundResultRepository;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayDisplayPort;
import org.nowstart.nyangnyangbot.domain.type.OverlayDisplayStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OverlayDisplayPersistenceAdapter implements OverlayDisplayPort {

    private final OverlayDisplayEventRepository overlayDisplayEventRepository;
    private final RouletteEventRepository rouletteEventRepository;
    private final RouletteRoundResultRepository rouletteRoundResultRepository;
    private final OutboundContractValidator contractValidator;

    @Override
    public void enqueueRouletteEvent(Long rouletteEventId, LocalDateTime expiresAt) {
        RouletteEvent rouletteEvent = rouletteEventRepository.findById(rouletteEventId)
                .orElseThrow(() -> new IllegalArgumentException("roulette event not found"));
        overlayDisplayEventRepository.save(OverlayDisplayEvent.builder()
                .rouletteEvent(rouletteEvent)
                .status(OverlayDisplayStatus.PENDING)
                .expiresAt(expiresAt)
                .build());
    }

    @Override
    public DisplayEventResult replayRouletteEvent(Long rouletteEventId, LocalDateTime expiresAt) {
        RouletteEvent rouletteEvent = rouletteEventRepository.findById(rouletteEventId)
                .orElseThrow(() -> new IllegalArgumentException("roulette event not found"));
        Long replayOf = overlayDisplayEventRepository.findByRouletteEventIdOrderByCreateDateDesc(rouletteEventId)
                .stream()
                .findFirst()
                .map(OverlayDisplayEvent::getId)
                .orElse(null);
        OverlayDisplayEvent saved = overlayDisplayEventRepository.save(OverlayDisplayEvent.builder()
                .rouletteEvent(rouletteEvent)
                .replayOfDisplayEventId(replayOf)
                .status(OverlayDisplayStatus.PENDING)
                .expiresAt(expiresAt)
                .build());
        return toModel(saved);
    }

    @Override
    public void markPendingExpiredBefore(LocalDateTime current) {
        overlayDisplayEventRepository.findByStatusAndExpiresAtBefore(OverlayDisplayStatus.PENDING, current)
                .forEach(OverlayDisplayEvent::markMissed);
    }

    @Override
    public Optional<DisplayEventResult> claimNextPending(LocalDateTime current) {
        return overlayDisplayEventRepository.findFirstByStatusAndExpiresAtAfterOrderByCreateDateAsc(
                        OverlayDisplayStatus.PENDING,
                        current
                )
                .map(displayEvent -> {
                    displayEvent.markDisplaying(current);
                    return toModel(displayEvent);
                });
    }

    @Override
    public void markDisplayed(Long displayEventId, LocalDateTime displayedAt) {
        OverlayDisplayEvent displayEvent = overlayDisplayEventRepository.findById(displayEventId)
                .orElseThrow(() -> new IllegalArgumentException("overlay display event not found"));
        displayEvent.markDisplayed(displayedAt);
    }

    private DisplayEventResult toModel(OverlayDisplayEvent displayEvent) {
        RouletteEvent event = displayEvent.getRouletteEvent();
        List<DisplayRoundResult> rounds = rouletteRoundResultRepository
                .findByRouletteEventIdOrderByRoundNoAsc(event.getId())
                .stream()
                .map(this::displayRoundResult)
                .toList();
        return contractValidator.persistenceResult("overlay.displayEventResult", new DisplayEventResult(
                displayEvent.getId(),
                event.getId(),
                event.getNickNameSnapshot(),
                event.getRoundCount(),
                displayEvent.getExpiresAt(),
                rounds
        ));
    }

    private DisplayRoundResult displayRoundResult(RouletteRoundResult entity) {
        return contractValidator.persistenceResult("overlay.displayRoundResult", new DisplayRoundResult(
                entity.getId(),
                entity.getRoundNo(),
                entity.getItemLabel(),
                entity.isLosingItem(),
                entity.getRewardType(),
                entity.getConversionMode(),
                entity.getExchangeFavoriteValue(),
                entity.getStatus(),
                entity.getLedgerId(),
                entity.getUserUpboId(),
                entity.getFailureReason()
        ));
    }
}
