package org.nowstart.nyangnyangbot.application.service.overlay;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.doReturn;
import static org.mockito.BDDMockito.given;
import org.mockito.BDDMockito;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.overlay.ManageOverlayDisplayUseCase.OverlayDisplayResult;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayDisplayPort;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayDisplayPort.DisplayEventResult;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayDisplayPort.DisplayRoundResult;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;

@ExtendWith(MockitoExtension.class)
class OverlayDisplayServiceTest {

    @Mock
    private OverlayTokenService overlayTokenService;

    @Mock
    private OverlayDisplayPort overlayDisplayPort;

    @Test
    void enqueue_ShouldCreatePendingDisplayEventWithExpiry() {
        // 준비
        OverlayDisplayService service = createService();
        doReturn(LocalDateTime.of(2026, 5, 9, 12, 0)).when(service).now();

        // 실행
        service.enqueueRouletteEvent(20L);

        // 검증
        BDDMockito.then(overlayDisplayPort).should().enqueueRouletteEvent(20L, LocalDateTime.of(2026, 5, 9, 12, 2));
    }

    @Test
    void replayRouletteEvent_ShouldReturnMappedDisplayEvent() {
        // 준비
        OverlayDisplayService service = createService();
        LocalDateTime now = LocalDateTime.of(2026, 5, 9, 12, 0);
        DisplayEventResult saved = displayEvent(3L, now.plusSeconds(120));
        doReturn(now).when(service).now();
        given(overlayDisplayPort.replayRouletteEvent(20L, now.plusSeconds(120))).willReturn(saved);

        // 실행
        OverlayDisplayResult result = service.replayRouletteEvent(20L);

        // 검증
        then(result.displayEventId()).isEqualTo(3L);
        then(result.maxAnimatedRounds()).isEqualTo(5);
        then(result.rounds()).hasSize(1);
    }

    @Test
    void claimNextEvent_ShouldMarkExpiredAsMissedAndReturnPendingEvent() {
        // 준비
        OverlayDisplayService service = createService();
        LocalDateTime now = LocalDateTime.of(2026, 5, 9, 12, 0);
        DisplayEventResult pending = displayEvent(2L, now.plusSeconds(60));
        given(overlayTokenService.validateToken("token")).willReturn(true);
        given(overlayDisplayPort.claimNextPending(now)).willReturn(Optional.of(pending));
        doReturn(now).when(service).now();

        // 실행
        Optional<OverlayDisplayResult> result = service.claimNextEvent("Bearer token");

        // 검증
        then(result).isPresent();
        then(result.get().displayEventId()).isEqualTo(2L);
        BDDMockito.then(overlayDisplayPort).should().markPendingExpiredBefore(now);
        BDDMockito.then(overlayDisplayPort).should().claimNextPending(now);
    }

    @Test
    void claimNextEvent_ShouldReturnEmptyWhenNoPendingEventExists() {
        // 준비
        OverlayDisplayService service = createService();
        LocalDateTime now = LocalDateTime.of(2026, 5, 9, 12, 0);
        given(overlayTokenService.validateToken("token")).willReturn(true);
        given(overlayDisplayPort.claimNextPending(now)).willReturn(Optional.empty());
        doReturn(now).when(service).now();

        // 실행
        Optional<OverlayDisplayResult> result = service.claimNextEvent("Bearer token");

        // 검증
        then(result).isEmpty();
        BDDMockito.then(overlayDisplayPort).should().markPendingExpiredBefore(now);
    }

    @Test
    void markDisplayed_ShouldValidateTokenAndDelegate() {
        // 준비
        OverlayDisplayService service = createService();
        LocalDateTime now = LocalDateTime.of(2026, 5, 9, 12, 0);
        given(overlayTokenService.validateToken("token")).willReturn(true);
        doReturn(now).when(service).now();

        // 실행
        service.markDisplayed(1L, "Bearer token");

        // 검증
        BDDMockito.then(overlayDisplayPort).should().markDisplayed(1L, now);
    }

    @Test
    void claimNextEvent_ShouldRejectBlankOrNonBearerAuthorization() {
        // 준비
        OverlayDisplayService service = createService();
        given(overlayTokenService.validateToken(null)).willReturn(false);

        // 실행 및 검증
        thenThrownBy(() -> service.claimNextEvent(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid overlay token");
        thenThrownBy(() -> service.claimNextEvent("token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid overlay token");
        BDDMockito.then(overlayDisplayPort).should(never()).claimNextPending(any());
    }

    @Test
    void markDisplayed_ShouldRejectInvalidToken() {
        // 준비
        OverlayDisplayService service = createService();
        given(overlayTokenService.validateToken("bad")).willReturn(false);

        // 실행 및 검증
        thenThrownBy(() -> service.markDisplayed(1L, "Bearer bad"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid overlay token");
    }

    private OverlayDisplayService createService() {
        return BDDMockito.spy(new OverlayDisplayService(
                overlayTokenService,
                overlayDisplayPort
        ));
    }

    private DisplayEventResult displayEvent(Long id, LocalDateTime expiresAt) {
        return new DisplayEventResult(id, 20L, "치즈냥", 1, expiresAt, List.of(round()));
    }

    private DisplayRoundResult round() {
        return new DisplayRoundResult(
                30L,
                1,
                "호감도 +10",
                false,
                RewardType.FAVORITE,
                ConversionMode.AUTO,
                10,
                RouletteRoundStatus.APPLIED,
                null,
                null,
                null
        );
    }
}
