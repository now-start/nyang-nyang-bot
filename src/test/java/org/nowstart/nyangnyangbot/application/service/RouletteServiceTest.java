package org.nowstart.nyangnyangbot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.model.RouletteEvent;
import org.nowstart.nyangnyangbot.application.model.RouletteItem;
import org.nowstart.nyangnyangbot.application.model.RouletteRound;
import org.nowstart.nyangnyangbot.application.model.RouletteTable;
import org.nowstart.nyangnyangbot.application.port.out.roulette.CreateRouletteEventCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.CreateRouletteRoundCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.dto.chzzk.DonationDto;
import org.nowstart.nyangnyangbot.application.dto.roulette.RouletteRunDto;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteEventStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;

@ExtendWith(MockitoExtension.class)
class RouletteServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RoulettePort roulettePort;

    @Mock
    private RouletteRoundApplyService rouletteRoundApplyService;

    @Mock
    private OverlayDisplayService overlayDisplayService;

    @Test
    void activateTable_ShouldRejectTableWithoutLosingItem() {
        RouletteService rouletteService = createService();
        given(roulettePort.findTableById(1L)).willReturn(Optional.of(table(false)));
        given(roulettePort.findActiveItemsByTableId(1L))
                .willReturn(List.of(item("호감도 +10", 10_000, false, 10)));

        assertThatThrownBy(() -> rouletteService.activateTable(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("losing item is required");
    }

    @Test
    void processDonation_ShouldIgnoreDonationWithoutExactCommandToken() {
        RouletteService rouletteService = createService();
        given(roulettePort.findLatestActiveTable()).willReturn(Optional.of(table(true)));

        RouletteRunDto.Response result = rouletteService.processDonation(new DonationDto(
                "donation-1",
                "CHAT",
                "channel-1",
                "user-1",
                "치즈냥",
                "5000",
                "안녕 !룰렛!",
                null
        ));

        assertThat(result.status()).isEqualTo("IGNORED");
        assertThat(result.reason()).isEqualTo("roulette command not found");
        then(roulettePort).should(never()).createEvent(any());
    }

    @Test
    void processDonation_ShouldSkipDuplicateDonationEventId() {
        RouletteService rouletteService = createService();
        given(roulettePort.findLatestActiveTable()).willReturn(Optional.of(table(true)));
        given(roulettePort.existsEventByDonationEventId("donation-1")).willReturn(true);

        RouletteRunDto.Response result = rouletteService.processDonation(new DonationDto(
                "donation-1",
                "CHAT",
                "channel-1",
                "user-1",
                "치즈냥",
                "5000",
                "안녕 !룰렛",
                null
        ));

        assertThat(result.status()).isEqualTo("DUPLICATE");
        then(rouletteRoundApplyService).should(never()).applyRound(any());
    }

    @Test
    void processDonation_ShouldConfirmRoundsAndApplyEachRound() {
        RouletteService rouletteService = createService();
        RouletteTable table = table(true);
        RouletteEvent event = event(20L, 2);
        List<RouletteRound> savedRounds = List.of(round(30L, 1), round(31L, 2));
        given(roulettePort.findLatestActiveTable()).willReturn(Optional.of(table));
        given(roulettePort.existsEventByDonationEventId("donation-1")).willReturn(false);
        given(roulettePort.findActiveItemsByTableId(1L))
                .willReturn(List.of(
                        item("호감도 +10", 9_999, false, 10),
                        item("꽝", 1, true, null)
                ));
        given(roulettePort.createEvent(any(CreateRouletteEventCommand.class))).willReturn(event);
        given(roulettePort.saveRounds(any(), any())).willReturn(savedRounds);
        given(roulettePort.findEventById(20L)).willReturn(Optional.of(event));
        given(roulettePort.findRoundsByEventId(20L)).willReturn(savedRounds);

        RouletteRunDto.Response result = rouletteService.processDonation(new DonationDto(
                "donation-1",
                "CHAT",
                "channel-1",
                "user-1",
                "치즈냥",
                "2500",
                "안녕 !룰렛",
                null
        ));

        assertThat(result.status()).isEqualTo("CONFIRMED");
        assertThat(result.roundCount()).isEqualTo(2);
        then(rouletteRoundApplyService).should().applyRound(30L);
        then(rouletteRoundApplyService).should().applyRound(31L);
        then(overlayDisplayService).should().enqueue(20L);
        ArgumentCaptor<CreateRouletteEventCommand> eventCaptor = ArgumentCaptor.forClass(CreateRouletteEventCommand.class);
        then(roulettePort).should().createEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().itemsSnapshotJson()).contains("호감도 +10", "꽝");
        ArgumentCaptor<List<CreateRouletteRoundCommand>> roundsCaptor = ArgumentCaptor.forClass(List.class);
        then(roulettePort).should().saveRounds(any(), roundsCaptor.capture());
        assertThat(roundsCaptor.getValue()).hasSize(2);
    }

    private RouletteService createService() {
        return new RouletteService(
                objectMapper,
                roulettePort,
                rouletteRoundApplyService,
                overlayDisplayService
        );
    }

    private RouletteTable table(boolean active) {
        return new RouletteTable(1L, "기본 룰렛", "!룰렛", 1_000L, active, 1, 100);
    }

    private RouletteItem item(String label, int probability, boolean losingItem, Integer favoriteValue) {
        return new RouletteItem(
                (long) probability,
                1L,
                label,
                probability,
                losingItem,
                favoriteValue == null ? RewardType.CUSTOM : RewardType.FAVORITE,
                favoriteValue == null ? ConversionMode.NONE : ConversionMode.AUTO,
                favoriteValue,
                true,
                probability
        );
    }

    private RouletteEvent event(Long id, int roundCount) {
        return new RouletteEvent(
                id,
                "donation-1",
                "user-1",
                "치즈냥",
                2_500L,
                "안녕 !룰렛",
                1L,
                1,
                "!룰렛",
                1_000L,
                roundCount,
                "[]",
                RouletteEventStatus.CONFIRMED,
                null
        );
    }

    private RouletteRound round(Long id, int roundNo) {
        return new RouletteRound(
                id,
                20L,
                "donation-1",
                "user-1",
                "치즈냥",
                roundNo,
                "호감도 +10",
                10_000,
                false,
                RewardType.FAVORITE,
                ConversionMode.AUTO,
                10,
                RouletteRoundStatus.CONFIRMED,
                null,
                null,
                null,
                roundNo
        );
    }
}
