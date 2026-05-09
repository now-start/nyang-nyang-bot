package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.doReturn;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.dto.overlay.OverlayDisplayDto;
import org.nowstart.nyangnyangbot.data.entity.OverlayDisplayEventEntity;
import org.nowstart.nyangnyangbot.data.entity.RouletteEventEntity;
import org.nowstart.nyangnyangbot.data.entity.RouletteRoundResultEntity;
import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.OverlayDisplayStatus;
import org.nowstart.nyangnyangbot.data.type.RewardType;
import org.nowstart.nyangnyangbot.data.type.RouletteRoundStatus;
import org.nowstart.nyangnyangbot.repository.OverlayDisplayEventRepository;
import org.nowstart.nyangnyangbot.repository.RouletteEventRepository;
import org.nowstart.nyangnyangbot.repository.RouletteRoundResultRepository;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OverlayDisplayServiceTest {

    @Mock
    private OverlayTokenService overlayTokenService;

    @Mock
    private OverlayDisplayEventRepository overlayDisplayEventRepository;

    @Mock
    private RouletteEventRepository rouletteEventRepository;

    @Mock
    private RouletteRoundResultRepository rouletteRoundResultRepository;

    @Test
    void enqueue_ShouldCreatePendingDisplayEventWithExpiry() {
        OverlayDisplayService service = createService();
        RouletteEventEntity rouletteEvent = rouletteEvent();
        given(rouletteEventRepository.findById(20L)).willReturn(Optional.of(rouletteEvent));
        given(overlayDisplayEventRepository.save(any(OverlayDisplayEventEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        doReturn(LocalDateTime.of(2026, 5, 9, 12, 0)).when(service).now();

        service.enqueue(20L);

        ArgumentCaptor<OverlayDisplayEventEntity> captor = ArgumentCaptor.forClass(OverlayDisplayEventEntity.class);
        then(overlayDisplayEventRepository).should().save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OverlayDisplayStatus.PENDING);
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(LocalDateTime.of(2026, 5, 9, 12, 2));
    }

    @Test
    void claimNextEvent_ShouldMarkExpiredAsMissedAndReturnPendingEvent() {
        OverlayDisplayService service = createService();
        LocalDateTime now = LocalDateTime.of(2026, 5, 9, 12, 0);
        OverlayDisplayEventEntity expired = displayEvent(1L, rouletteEvent(), now.minusSeconds(1));
        OverlayDisplayEventEntity pending = displayEvent(2L, rouletteEvent(), now.plusSeconds(60));
        given(overlayTokenService.validateToken("token")).willReturn(true);
        given(overlayDisplayEventRepository.findByStatusAndExpiresAtBefore(OverlayDisplayStatus.PENDING, now))
                .willReturn(List.of(expired));
        given(overlayDisplayEventRepository.findFirstByStatusAndExpiresAtAfterOrderByCreateDateAsc(
                OverlayDisplayStatus.PENDING,
                now
        )).willReturn(Optional.of(pending));
        given(rouletteRoundResultRepository.findByRouletteEventIdOrderByRoundNoAsc(20L))
                .willReturn(List.of(round()));
        doReturn(now).when(service).now();

        Optional<OverlayDisplayDto.EventResponse> result = service.claimNextEvent("Bearer token");

        assertThat(result).isPresent();
        assertThat(result.get().displayEventId()).isEqualTo(2L);
        assertThat(expired.getStatus()).isEqualTo(OverlayDisplayStatus.MISSED);
        assertThat(pending.getStatus()).isEqualTo(OverlayDisplayStatus.DISPLAYING);
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
                overlayDisplayEventRepository,
                rouletteEventRepository,
                rouletteRoundResultRepository
        ));
    }

    private OverlayDisplayEventEntity displayEvent(Long id, RouletteEventEntity rouletteEvent, LocalDateTime expiresAt) {
        OverlayDisplayEventEntity event = OverlayDisplayEventEntity.builder()
                .id(id)
                .rouletteEvent(rouletteEvent)
                .status(OverlayDisplayStatus.PENDING)
                .expiresAt(expiresAt)
                .build();
        return event;
    }

    private RouletteEventEntity rouletteEvent() {
        return RouletteEventEntity.builder()
                .id(20L)
                .donationEventId("donation-1")
                .userId("user-1")
                .nickNameSnapshot("치즈냥")
                .roundCount(1)
                .build();
    }

    private RouletteRoundResultEntity round() {
        RouletteRoundResultEntity round = RouletteRoundResultEntity.builder()
                .id(30L)
                .roundNo(1)
                .itemLabel("호감도 +10")
                .losingItem(false)
                .rewardType(RewardType.FAVORITE)
                .conversionMode(ConversionMode.AUTO)
                .exchangeFavoriteValue(10)
                .status(RouletteRoundStatus.APPLIED)
                .build();
        ReflectionTestUtils.setField(round, "rouletteEvent", rouletteEvent());
        return round;
    }
}
