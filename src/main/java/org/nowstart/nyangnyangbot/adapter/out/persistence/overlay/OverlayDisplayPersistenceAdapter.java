package org.nowstart.nyangnyangbot.adapter.out.persistence.overlay;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayDisplayPort.DisplayEventResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayDisplayPort;
import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.entity.OverlayDisplayEventEntity;
import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.repository.OverlayDisplayEventRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteEventEntity;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteRoundResultEntity;
import org.nowstart.nyangnyangbot.domain.type.OverlayDisplayStatus;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteEventRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRoundResultRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OverlayDisplayPersistenceAdapter implements OverlayDisplayPort {

    private final OverlayDisplayEventRepository overlayDisplayEventRepository;
    private final RouletteEventRepository rouletteEventRepository;
    private final RouletteRoundResultRepository rouletteRoundResultRepository;

    @Override
    public void enqueueRouletteEvent(Long rouletteEventId, LocalDateTime expiresAt) {
        RouletteEventEntity rouletteEvent = rouletteEventRepository.findById(rouletteEventId)
                .orElseThrow(() -> new IllegalArgumentException("roulette event not found"));
        overlayDisplayEventRepository.save(OverlayDisplayEventEntity.builder()
                .rouletteEvent(rouletteEvent)
                .status(OverlayDisplayStatus.PENDING)
                .expiresAt(expiresAt)
                .build());
    }

    @Override
    public DisplayEventResult replayRouletteEvent(Long rouletteEventId, LocalDateTime expiresAt) {
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
                .expiresAt(expiresAt)
                .build());
        return toModel(saved);
    }

    @Override
    public void markPendingExpiredBefore(LocalDateTime current) {
        overlayDisplayEventRepository.findByStatusAndExpiresAtBefore(OverlayDisplayStatus.PENDING, current)
                .forEach(OverlayDisplayEventEntity::markMissed);
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
        OverlayDisplayEventEntity displayEvent = overlayDisplayEventRepository.findById(displayEventId)
                .orElseThrow(() -> new IllegalArgumentException("overlay display event not found"));
        displayEvent.markDisplayed(displayedAt);
    }

    private DisplayEventResult toModel(OverlayDisplayEventEntity displayEvent) {
        RouletteEventEntity event = displayEvent.getRouletteEvent();
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

    private RoundResult toRound(RouletteRoundResultEntity entity) {
        RouletteEventEntity event = entity.getRouletteEvent();
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
