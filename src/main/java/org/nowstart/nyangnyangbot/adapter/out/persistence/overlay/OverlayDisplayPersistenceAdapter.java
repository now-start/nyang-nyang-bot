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
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayDisplayPort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;
import org.nowstart.nyangnyangbot.domain.type.OverlayDisplayStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OverlayDisplayPersistenceAdapter implements OverlayDisplayPort {

    private final OverlayDisplayEventRepository overlayDisplayEventRepository;
    private final RouletteEventRepository rouletteEventRepository;
    private final RouletteRoundResultRepository rouletteRoundResultRepository;

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
        List<RoundResult> rounds = rouletteRoundResultRepository.findByRouletteEventIdOrderByRoundNoAsc(event.getId())
                .stream()
                .map(this::toRound)
                .toList();
        return new DisplayEventResult(
                displayEvent.getId(),
                event.getId(),
                event.getNickNameSnapshot(),
                event.getRoundCount(),
                displayEvent.getExpiresAt(),
                rounds
        );
    }

    private RoundResult toRound(RouletteRoundResult entity) {
        RouletteEvent event = entity.getRouletteEvent();
        return new RoundResult(
                entity.getId(),
                event == null ? null : event.getId(),
                event == null ? null : event.getDonationEventId(),
                event == null ? null : event.getUserId(),
                event == null ? null : event.getNickNameSnapshot(),
                entity.getRoundNo(),
                entity.getItemLabel(),
                entity.getProbabilityBasisPoints(),
                entity.isLosingItem(),
                entity.getRewardType(),
                entity.getConversionMode(),
                entity.getExchangeFavoriteValue(),
                entity.getStatus(),
                entity.getLedgerId(),
                entity.getUserUpboId(),
                entity.getFailureReason(),
                entity.getTicket()
        );
    }
}
