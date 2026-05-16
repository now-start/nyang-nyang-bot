package org.nowstart.nyangnyangbot.adapter.out.persistence.overlay;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.entity.OverlayDisplayEventEntity;
import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.repository.OverlayDisplayEventRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteEventEntity;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteRoundResultEntity;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteEventRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRoundResultRepository;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayDisplayPort.DisplayEventResult;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.OverlayDisplayStatus;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteEventStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;

@ExtendWith(MockitoExtension.class)
class OverlayDisplayPersistenceAdapterTest {

    @Mock
    private OverlayDisplayEventRepository overlayDisplayEventRepository;

    @Mock
    private RouletteEventRepository rouletteEventRepository;

    @Mock
    private RouletteRoundResultRepository rouletteRoundResultRepository;

    @Test
    void enqueueRouletteEvent_ShouldSavePendingDisplayEvent() {
        // 준비
        OverlayDisplayPersistenceAdapter adapter = adapter();
        LocalDateTime expiresAt = LocalDateTime.of(2026, 5, 16, 23, 0);
        given(rouletteEventRepository.findById(1L)).willReturn(Optional.of(event(1L)));

        // 실행
        adapter.enqueueRouletteEvent(1L, expiresAt);

        // 검증
        BDDMockito.then(overlayDisplayEventRepository).should().save(any(OverlayDisplayEventEntity.class));
    }

    @Test
    void enqueueRouletteEvent_ShouldRejectMissingRouletteEvent() {
        // 준비
        OverlayDisplayPersistenceAdapter adapter = adapter();
        given(rouletteEventRepository.findById(404L)).willReturn(Optional.empty());

        // 실행 및 검증
        thenThrownBy(() -> adapter.enqueueRouletteEvent(404L, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("roulette event not found");
    }

    @Test
    void replayRouletteEvent_ShouldLinkPreviousDisplayEventAndMapRounds() {
        // 준비
        OverlayDisplayPersistenceAdapter adapter = adapter();
        LocalDateTime expiresAt = LocalDateTime.of(2026, 5, 16, 23, 10);
        RouletteEventEntity event = event(1L);
        OverlayDisplayEventEntity previous = displayEvent(10L, event, OverlayDisplayStatus.DISPLAYED, expiresAt);
        OverlayDisplayEventEntity saved = displayEvent(11L, event, OverlayDisplayStatus.PENDING, expiresAt);
        given(rouletteEventRepository.findById(1L)).willReturn(Optional.of(event));
        given(overlayDisplayEventRepository.findByRouletteEventIdOrderByCreateDateDesc(1L)).willReturn(List.of(previous));
        given(overlayDisplayEventRepository.save(any(OverlayDisplayEventEntity.class))).willReturn(saved);
        given(rouletteRoundResultRepository.findByRouletteEventIdOrderByRoundNoAsc(1L))
                .willReturn(List.of(round(20L, event)));

        // 실행
        DisplayEventResult result = adapter.replayRouletteEvent(1L, expiresAt);

        // 검증
        then(result.id()).isEqualTo(11L);
        then(result.rouletteEventId()).isEqualTo(1L);
        then(result.rounds()).hasSize(1);
        then(result.rounds().getFirst().itemLabel()).isEqualTo("당첨");
    }

    @Test
    void replayRouletteEvent_ShouldAllowFirstReplayWithoutPreviousEvent() {
        // 준비
        OverlayDisplayPersistenceAdapter adapter = adapter();
        LocalDateTime expiresAt = LocalDateTime.of(2026, 5, 16, 23, 20);
        RouletteEventEntity event = event(1L);
        OverlayDisplayEventEntity saved = displayEvent(12L, event, OverlayDisplayStatus.PENDING, expiresAt);
        given(rouletteEventRepository.findById(1L)).willReturn(Optional.of(event));
        given(overlayDisplayEventRepository.findByRouletteEventIdOrderByCreateDateDesc(1L)).willReturn(List.of());
        given(overlayDisplayEventRepository.save(any(OverlayDisplayEventEntity.class))).willReturn(saved);
        given(rouletteRoundResultRepository.findByRouletteEventIdOrderByRoundNoAsc(1L)).willReturn(List.of());

        // 실행
        DisplayEventResult result = adapter.replayRouletteEvent(1L, expiresAt);

        // 검증
        then(result.id()).isEqualTo(12L);
        then(result.rounds()).isEmpty();
    }

    @Test
    void displayStateChanges_ShouldExpireClaimAndMarkDisplayed() {
        // 준비
        OverlayDisplayPersistenceAdapter adapter = adapter();
        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 23, 30);
        RouletteEventEntity event = event(1L);
        OverlayDisplayEventEntity expired = displayEvent(13L, event, OverlayDisplayStatus.PENDING, now.minusMinutes(1));
        OverlayDisplayEventEntity pending = displayEvent(14L, event, OverlayDisplayStatus.PENDING, now.plusMinutes(1));
        given(overlayDisplayEventRepository.findByStatusAndExpiresAtBefore(OverlayDisplayStatus.PENDING, now))
                .willReturn(List.of(expired));
        given(overlayDisplayEventRepository.findFirstByStatusAndExpiresAtAfterOrderByCreateDateAsc(
                OverlayDisplayStatus.PENDING,
                now
        )).willReturn(Optional.of(pending));
        given(overlayDisplayEventRepository.findById(14L)).willReturn(Optional.of(pending));
        given(rouletteRoundResultRepository.findByRouletteEventIdOrderByRoundNoAsc(1L)).willReturn(List.of());

        // 실행
        adapter.markPendingExpiredBefore(now);
        Optional<DisplayEventResult> claimed = adapter.claimNextPending(now);
        adapter.markDisplayed(14L, now.plusSeconds(1));

        // 검증
        then(expired.getStatus()).isEqualTo(OverlayDisplayStatus.MISSED);
        then(claimed).isPresent();
        then(pending.getStatus()).isEqualTo(OverlayDisplayStatus.DISPLAYED);
    }

    @Test
    void markDisplayed_ShouldRejectMissingDisplayEvent() {
        // 준비
        OverlayDisplayPersistenceAdapter adapter = adapter();
        given(overlayDisplayEventRepository.findById(404L)).willReturn(Optional.empty());

        // 실행 및 검증
        thenThrownBy(() -> adapter.markDisplayed(404L, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("overlay display event not found");
    }

    private OverlayDisplayPersistenceAdapter adapter() {
        return new OverlayDisplayPersistenceAdapter(
                overlayDisplayEventRepository,
                rouletteEventRepository,
                rouletteRoundResultRepository
        );
    }

    private RouletteEventEntity event(Long id) {
        return RouletteEventEntity.builder()
                .id(id)
                .donationEventId("donation-1")
                .idempotencyKey("donation-1")
                .userId("user-1")
                .nickNameSnapshot("치즈냥")
                .donationAmount(1_000L)
                .donationText("!룰렛")
                .rouletteTableId(1L)
                .rouletteTableVersion(1)
                .command("!룰렛")
                .pricePerRound(1_000L)
                .roundCount(1)
                .itemsSnapshotJson("[]")
                .status(RouletteEventStatus.CONFIRMED)
                .build();
    }

    private OverlayDisplayEventEntity displayEvent(
            Long id,
            RouletteEventEntity event,
            OverlayDisplayStatus status,
            LocalDateTime expiresAt
    ) {
        return OverlayDisplayEventEntity.builder()
                .id(id)
                .rouletteEvent(event)
                .status(status)
                .expiresAt(expiresAt)
                .build();
    }

    private RouletteRoundResultEntity round(Long id, RouletteEventEntity event) {
        return RouletteRoundResultEntity.builder()
                .id(id)
                .rouletteEvent(event)
                .roundNo(1)
                .itemLabel("당첨")
                .probabilityBasisPoints(10_000)
                .losingItem(false)
                .rewardType(RewardType.FAVORITE)
                .conversionMode(ConversionMode.AUTO)
                .exchangeFavoriteValue(100)
                .status(RouletteRoundStatus.CONFIRMED)
                .ticket(777)
                .build();
    }
}
