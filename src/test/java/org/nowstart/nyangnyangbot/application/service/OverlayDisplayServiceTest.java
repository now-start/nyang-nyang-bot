package org.nowstart.nyangnyangbot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.doReturn;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.domain.model.OverlayDisplayEvent;
import org.nowstart.nyangnyangbot.domain.model.RouletteRound;
import org.nowstart.nyangnyangbot.application.gateway.out.overlay.OverlayDisplayPort;
import org.nowstart.nyangnyangbot.application.dto.overlay.OverlayDisplayDto;
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
        OverlayDisplayService service = createService();
        doReturn(LocalDateTime.of(2026, 5, 9, 12, 0)).when(service).now();

        service.enqueue(20L);

        then(overlayDisplayPort).should().enqueueRouletteEvent(20L, LocalDateTime.of(2026, 5, 9, 12, 2));
    }

    @Test
    void claimNextEvent_ShouldMarkExpiredAsMissedAndReturnPendingEvent() {
        OverlayDisplayService service = createService();
        LocalDateTime now = LocalDateTime.of(2026, 5, 9, 12, 0);
        OverlayDisplayEvent pending = displayEvent(2L, now.plusSeconds(60));
        given(overlayTokenService.validateToken("token")).willReturn(true);
        given(overlayDisplayPort.claimNextPending(now)).willReturn(Optional.of(pending));
        doReturn(now).when(service).now();

        Optional<OverlayDisplayDto.EventResponse> result = service.claimNextEvent("Bearer token");

        assertThat(result).isPresent();
        assertThat(result.get().displayEventId()).isEqualTo(2L);
        then(overlayDisplayPort).should().markPendingExpiredBefore(now);
        then(overlayDisplayPort).should().claimNextPending(now);
    }

    @Test
    void markDisplayed_ShouldRejectInvalidToken() {
        OverlayDisplayService service = createService();
        given(overlayTokenService.validateToken("bad")).willReturn(false);

        assertThatThrownBy(() -> service.markDisplayed(1L, "Bearer bad"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid overlay token");
    }

    private OverlayDisplayService createService() {
        return BDDMockito.spy(new OverlayDisplayService(
                overlayTokenService,
                overlayDisplayPort
        ));
    }

    private OverlayDisplayEvent displayEvent(Long id, LocalDateTime expiresAt) {
        return new OverlayDisplayEvent(id, 20L, "치즈냥", 1, expiresAt, List.of(round()));
    }

    private RouletteRound round() {
        return new RouletteRound(
                30L,
                20L,
                "donation-1",
                "user-1",
                "치즈냥",
                1,
                "호감도 +10",
                10_000,
                false,
                RewardType.FAVORITE,
                ConversionMode.AUTO,
                10,
                RouletteRoundStatus.APPLIED,
                null,
                null,
                null,
                1
        );
    }
}
