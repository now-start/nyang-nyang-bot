package org.nowstart.nyangnyangbot.application.service.roulette;

import org.nowstart.nyangnyangbot.application.service.overlay.OverlayDisplayService;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import org.mockito.BDDMockito;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.EventResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.ItemResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.TableResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ProcessRouletteDonationUseCase.RouletteRunResult;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.CommandRecord;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.DonationEventPayload;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.CreateRouletteEventCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.CreateRouletteRoundCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.domain.type.CommandActionKey;
import org.nowstart.nyangnyangbot.domain.type.CommandType;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteEventStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class RouletteApplicationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RoulettePort roulettePort;

    @Mock
    private CommandPort commandPort;

    @Mock
    private RouletteRoundApplyService rouletteRoundApplyService;

    @Mock
    private OverlayDisplayService overlayDisplayService;

    @Test
    void activateTable_ShouldRejectTableWithoutLosingItem() {
        // 준비
        ManageRouletteService rouletteService = createManageService();
        given(roulettePort.findTableById(1L)).willReturn(Optional.of(table(false)));
        given(roulettePort.findActiveItemsByTableId(1L))
                .willReturn(List.of(item("호감도 +10", 10_000, false, 10)));

        // 실행 및 검증
        thenThrownBy(() -> rouletteService.activateTable(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("losing item is required");
    }

    @Test
    void processDonation_ShouldIgnoreNullDonation() {
        // 준비
        ProcessRouletteDonationService rouletteService = createProcessService();

        // 실행
        RouletteRunResult result = rouletteService.processDonation(null);

        // 검증
        then(result.status().name()).isEqualTo("IGNORED");
        then(result.reason()).isEqualTo("donation is required");
        BDDMockito.then(roulettePort).shouldHaveNoInteractions();
    }

    @Test
    void processDonation_ShouldIgnoreBlankDonationEventId() {
        // 준비
        ProcessRouletteDonationService rouletteService = createProcessService();

        // 실행
        RouletteRunResult result = rouletteService.processDonation(new DonationEventPayload(
                " ",
                "CHAT",
                "channel-1",
                "user-1",
                "치즈냥",
                "5000",
                "!룰렛",
                null
        ));

        // 검증
        then(result.status().name()).isEqualTo("IGNORED");
        then(result.reason()).isEqualTo("donation event id is required");
        BDDMockito.then(roulettePort).shouldHaveNoInteractions();
    }

    @Test
    void processDonation_ShouldIgnoreWhenActiveTableIsMissing() {
        // 준비
        ProcessRouletteDonationService rouletteService = createProcessService();
        given(roulettePort.findLatestActiveTable()).willReturn(Optional.empty());

        // 실행
        RouletteRunResult result = rouletteService.processDonation(new DonationEventPayload(
                "donation-1",
                "CHAT",
                "channel-1",
                "user-1",
                "치즈냥",
                "5000",
                "!룰렛",
                null
        ));

        // 검증
        then(result.status().name()).isEqualTo("IGNORED");
        then(result.reason()).isEqualTo("active roulette table not found");
        BDDMockito.then(roulettePort).should(never()).existsEventByDonationEventId(any());
    }

    @Test
    void processDonation_ShouldIgnoreDonationWithoutExactCommandToken() {
        // 준비
        ProcessRouletteDonationService rouletteService = createProcessService();
        given(roulettePort.findLatestActiveTable()).willReturn(Optional.of(table(true)));

        // 실행
        RouletteRunResult result = rouletteService.processDonation(new DonationEventPayload(
                "donation-1",
                "CHAT",
                "channel-1",
                "user-1",
                "치즈냥",
                "5000",
                "안녕 !룰렛!",
                null
        ));

        // 검증
        then(result.status().name()).isEqualTo("IGNORED");
        then(result.reason()).isEqualTo("roulette command not found");
        BDDMockito.then(roulettePort).should(never()).createEvent(any());
    }

    @Test
    void processDonation_ShouldSkipDuplicateDonationEventId() {
        // 준비
        ProcessRouletteDonationService rouletteService = createProcessService();
        given(roulettePort.findLatestActiveTable()).willReturn(Optional.of(table(true)));
        given(roulettePort.existsEventByDonationEventId("donation-1")).willReturn(true);

        // 실행
        RouletteRunResult result = rouletteService.processDonation(new DonationEventPayload(
                "donation-1",
                "CHAT",
                "channel-1",
                "user-1",
                "치즈냥",
                "5000",
                "안녕 !룰렛",
                null
        ));

        // 검증
        then(result.status().name()).isEqualTo("DUPLICATE");
        BDDMockito.then(rouletteRoundApplyService).should(never()).applyRound(any());
    }

    @Test
    void processDonation_ShouldIgnoreWhenDonationAmountIsLessThanRoundPrice() {
        // 준비
        ProcessRouletteDonationService rouletteService = createProcessService();
        given(roulettePort.findLatestActiveTable()).willReturn(Optional.of(table(true)));
        given(roulettePort.existsEventByDonationEventId("donation-1")).willReturn(false);

        // 실행
        RouletteRunResult result = rouletteService.processDonation(new DonationEventPayload(
                "donation-1",
                "CHAT",
                "channel-1",
                "user-1",
                "치즈냥",
                "999",
                "안녕 !룰렛",
                null
        ));

        // 검증
        then(result.status().name()).isEqualTo("IGNORED");
        then(result.reason()).isEqualTo("donation amount is less than roulette price");
        BDDMockito.then(roulettePort).should(never()).findActiveItemsByTableId(any());
    }

    @Test
    void processDonation_ShouldIgnoreWhenActiveTableValidationFails() {
        // 준비
        ProcessRouletteDonationService rouletteService = createProcessService();
        given(roulettePort.findLatestActiveTable()).willReturn(Optional.of(table(true)));
        given(roulettePort.existsEventByDonationEventId("donation-1")).willReturn(false);
        given(roulettePort.findActiveItemsByTableId(1L))
                .willReturn(List.of(item("호감도 +10", 1_000, false, 10)));

        // 실행
        RouletteRunResult result = rouletteService.processDonation(new DonationEventPayload(
                "donation-1",
                "CHAT",
                "channel-1",
                "user-1",
                "치즈냥",
                "1000",
                "안녕 !룰렛",
                null
        ));

        // 검증
        then(result.status().name()).isEqualTo("IGNORED");
        then(result.reason()).isEqualTo("active roulette table is invalid");
        BDDMockito.then(roulettePort).should(never()).createEvent(any());
    }

    @Test
    void processDonation_ShouldConfirmRoundsAndApplyEachRound() {
        // 준비
        ProcessRouletteDonationService rouletteService = createProcessService();
        TableResult table = table(true);
        EventResult event = event(20L, 2);
        List<RoundResult> savedRounds = List.of(round(30L, 1), round(31L, 2));
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

        // 실행
        RouletteRunResult result = rouletteService.processDonation(new DonationEventPayload(
                "donation-1",
                "CHAT",
                "channel-1",
                "user-1",
                "치즈냥",
                "2500",
                "안녕 !룰렛",
                null
        ));

        // 검증
        then(result.status().name()).isEqualTo("CONFIRMED");
        then(result.roundCount()).isEqualTo(2);
        BDDMockito.then(rouletteRoundApplyService).should().applyRound(30L);
        BDDMockito.then(rouletteRoundApplyService).should().applyRound(31L);
        BDDMockito.then(overlayDisplayService).should().enqueueRouletteEvent(20L);
        ArgumentCaptor<CreateRouletteEventCommand> eventCaptor = ArgumentCaptor.forClass(CreateRouletteEventCommand.class);
        BDDMockito.then(roulettePort).should().createEvent(eventCaptor.capture());
        then(eventCaptor.getValue().itemsSnapshotJson()).contains("호감도 +10", "꽝");
        ArgumentCaptor<List<CreateRouletteRoundCommand>> roundsCaptor = ArgumentCaptor.forClass(List.class);
        BDDMockito.then(roulettePort).should().saveRounds(any(), roundsCaptor.capture());
        then(roundsCaptor.getValue()).hasSize(2);
    }

    @Test
    void processDonation_ShouldPropagateRoundApplyFailureAndSkipOverlayQueue() {
        // 준비
        ProcessRouletteDonationService rouletteService = createProcessService();
        TableResult table = table(true);
        EventResult event = event(20L, 1);
        List<RoundResult> savedRounds = List.of(round(30L, 1));
        given(roulettePort.findLatestActiveTable()).willReturn(Optional.of(table));
        given(roulettePort.existsEventByDonationEventId("donation-1")).willReturn(false);
        given(roulettePort.findActiveItemsByTableId(1L))
                .willReturn(List.of(
                        item("호감도 +10", 9_999, false, 10),
                        item("꽝", 1, true, null)
                ));
        given(roulettePort.createEvent(any(CreateRouletteEventCommand.class))).willReturn(event);
        given(roulettePort.saveRounds(any(), any())).willReturn(savedRounds);
        BDDMockito.willThrow(new IllegalStateException("룰렛 보상 반영 실패"))
                .given(rouletteRoundApplyService)
                .applyRound(30L);

        // 실행 및 검증
        thenThrownBy(() -> rouletteService.processDonation(new DonationEventPayload(
                "donation-1",
                "CHAT",
                "channel-1",
                "user-1",
                "치즈냥",
                "2500",
                "안녕 !룰렛",
                null
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("룰렛 보상 반영 실패");
        BDDMockito.then(overlayDisplayService).should(never()).enqueueRouletteEvent(any());
    }

    @Test
    void processDonation_ShouldConfirmEvenWhenRoundCountExceedsHighThreshold() {
        // 준비
        ProcessRouletteDonationService rouletteService = createProcessService();
        TableResult table = table(true, 1);
        EventResult event = event(21L, 2);
        List<RoundResult> savedRounds = List.of(round(32L, 1), round(33L, 2));
        given(roulettePort.findLatestActiveTable()).willReturn(Optional.of(table));
        given(roulettePort.existsEventByDonationEventId("donation-1")).willReturn(false);
        given(roulettePort.findActiveItemsByTableId(1L))
                .willReturn(List.of(
                        item("호감도 +10", 9_999, false, 10),
                        item("꽝", 1, true, null)
                ));
        given(roulettePort.createEvent(any(CreateRouletteEventCommand.class))).willReturn(event);
        given(roulettePort.saveRounds(any(), any())).willReturn(savedRounds);
        given(roulettePort.findEventById(21L)).willReturn(Optional.of(event));
        given(roulettePort.findRoundsByEventId(21L)).willReturn(savedRounds);

        // 실행
        RouletteRunResult result = rouletteService.processDonation(new DonationEventPayload(
                "donation-1",
                "CHAT",
                "channel-1",
                "user-1",
                "치즈냥",
                "2500",
                "안녕 !룰렛",
                null
        ));

        // 검증
        then(result.status().name()).isEqualTo("CONFIRMED");
        then(result.roundCount()).isEqualTo(2);
        BDDMockito.then(overlayDisplayService).should().enqueueRouletteEvent(21L);
    }

    @Test
    void processDonation_ShouldRunWithinTransaction() throws NoSuchMethodException {
        // 실행
        Transactional transactional = ProcessRouletteDonationService.class
                .getMethod("processDonation", DonationEventPayload.class)
                .getAnnotation(Transactional.class);

        // 검증
        then(transactional).isNotNull();
    }

    @Test
    void applyRound_ShouldJoinCallerTransaction() throws NoSuchMethodException {
        // 실행
        Transactional transactional = RouletteRoundApplyService.class
                .getMethod("applyRound", Long.class)
                .getAnnotation(Transactional.class);

        // 검증
        then(transactional).isNotNull();
        then(transactional.propagation()).isEqualTo(Propagation.REQUIRED);
    }

    private ManageRouletteService createManageService() {
        return new ManageRouletteService(roulettePort, commandPort);
    }

    private ProcessRouletteDonationService createProcessService() {
        lenient().when(commandPort.findActiveByActionKey(CommandActionKey.ROULETTE_DONATION))
                .thenReturn(Optional.of(rouletteDonationCommand()));
        return new ProcessRouletteDonationService(
                objectMapper,
                commandPort,
                roulettePort,
                rouletteRoundApplyService,
                new RouletteEventStatusService(roulettePort),
                overlayDisplayService
        );
    }

    private TableResult table(boolean active) {
        return table(active, 100);
    }

    private TableResult table(boolean active, Integer highRoundThreshold) {
        return new TableResult(1L, "기본 룰렛", "!룰렛", 1_000L, active, 1, highRoundThreshold);
    }

    private CommandRecord rouletteDonationCommand() {
        return new CommandRecord(
                100L,
                CommandType.TRIGGER,
                "!룰렛",
                CommandActionKey.ROULETTE_DONATION,
                null,
                null,
                null,
                true,
                "USER",
                0,
                "system",
                "system",
                null,
                null
        );
    }

    private ItemResult item(String label, int probability, boolean losingItem, Integer favoriteValue) {
        return new ItemResult(
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

    private EventResult event(Long id, int roundCount) {
        return new EventResult(
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

    private RoundResult round(Long id, int roundNo) {
        return new RoundResult(
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
