package org.nowstart.nyangnyangbot.application.service.roulette;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.annotation.Transactional;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.DonationReceived;
import org.nowstart.nyangnyangbot.application.port.in.overlay.QueueOverlayDisplayUseCase;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.ConfigResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.OptionResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RunResult;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteConfigStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRunStatus;

class ProcessRouletteDonationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-23T00:00:00Z");

    @Test
    void donationPreparation_HoldsOneTransactionAcrossActiveConfigLockAndRunCreation()
            throws NoSuchMethodException {
        Transactional transactional = ProcessRouletteDonationService.class
                .getMethod("processDonation", Long.class, DonationReceived.class)
                .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
    }

    @Test
    void createsReadyRunWithoutApplyingRewardsBeforeDonationCommit() {
        RoulettePort port = Mockito.mock(RoulettePort.class);
        RouletteRoundApplyService applyService = Mockito.mock(RouletteRoundApplyService.class);
        QueueOverlayDisplayUseCase overlay = Mockito.mock(QueueOverlayDisplayUseCase.class);
        ProcessRouletteDonationService service = new ProcessRouletteDonationService(port, applyService, overlay) {
            @Override
            Instant now() {
                return NOW;
            }
        };
        ConfigResult config = new ConfigResult(
                1L, "기본", "!룰렛", 1_000L, 100, RouletteConfigStatus.ACTIVE, NOW, NOW
        );
        List<OptionResult> options = List.of(
                option(1L, 7_000, false),
                option(2L, 3_000, true)
        );
        given(port.findActiveConfigForUpdate()).willReturn(Optional.of(config));
        given(port.findOptionsByConfigId(1L)).willReturn(options);
        given(port.createReadyRun(Mockito.any())).willReturn(new RunResult(
                7L, 1L, "event-1", "user-1", "시청자", 1_000L, RouletteRunStatus.READY, NOW, NOW
        ));

        var result = service.processDonation(7L, donation());

        then(applyService).shouldHaveNoInteractions();
        then(overlay).shouldHaveNoInteractions();
        assertThat(result).contains(7L);
    }

    @Test
    void recoverPreparedRunAppliesRoundsAndQueuesOverlay() {
        RoulettePort port = Mockito.mock(RoulettePort.class);
        RouletteRoundApplyService applyService = Mockito.mock(RouletteRoundApplyService.class);
        QueueOverlayDisplayUseCase overlay = Mockito.mock(QueueOverlayDisplayUseCase.class);
        ProcessRouletteDonationService service = new ProcessRouletteDonationService(port, applyService, overlay);
        given(port.existsRun(7L)).willReturn(true);
        given(port.findRoundsByRunId(7L)).willReturn(List.of(round(RouletteRoundStatus.CONFIRMED)));

        service.recoverRun(7L);

        then(applyService).should().applyRound(10L);
        then(overlay).should().enqueueRouletteRun(7L);
    }

    @Test
    void anonymousDonorIsNotEligibleForRoulette() {
        RoulettePort port = Mockito.mock(RoulettePort.class);
        ProcessRouletteDonationService service = new ProcessRouletteDonationService(
                port,
                Mockito.mock(RouletteRoundApplyService.class),
                Mockito.mock(QueueOverlayDisplayUseCase.class)
        );
        DonationReceived anonymous = new DonationReceived(
                "event-1", "CHAT", "streamer-1", null, "익명", "1000", "!룰렛", Map.of()
        );

        var result = service.processDonation(7L, anonymous);

        assertThat(result).isEmpty();
        then(port).shouldHaveNoInteractions();
    }

    @Test
    void duplicateDonationDefersRecoveryUntilAfterCallerCommits() {
        RoulettePort port = Mockito.mock(RoulettePort.class);
        RouletteRoundApplyService applyService = Mockito.mock(RouletteRoundApplyService.class);
        QueueOverlayDisplayUseCase overlay = Mockito.mock(QueueOverlayDisplayUseCase.class);
        ProcessRouletteDonationService service = new ProcessRouletteDonationService(port, applyService, overlay);
        given(port.existsRun(7L)).willReturn(true);

        var result = service.processDonation(7L, donation());

        assertThat(result).contains(7L);
        then(applyService).shouldHaveNoInteractions();
        then(overlay).shouldHaveNoInteractions();
    }

    @Test
    void recoveryLeavesTransientDatabaseFailureConfirmedForNextAttempt() {
        RoulettePort port = Mockito.mock(RoulettePort.class);
        RouletteRoundApplyService applyService = Mockito.mock(RouletteRoundApplyService.class);
        QueueOverlayDisplayUseCase overlay = Mockito.mock(QueueOverlayDisplayUseCase.class);
        ProcessRouletteDonationService service = new ProcessRouletteDonationService(port, applyService, overlay);
        given(port.findMaxRunIdNeedingRecovery()).willReturn(7L);
        given(port.findRunIdsNeedingRecovery(Mockito.anyLong(), Mockito.eq(100))).willReturn(List.of(7L));
        given(port.findRoundsByRunId(7L)).willReturn(List.of(round(RouletteRoundStatus.CONFIRMED)));
        Mockito.doThrow(new CannotAcquireLockException("retry"))
                .when(applyService).applyRound(10L);

        assertThat(service.recoverPendingRuns(100)).isZero();

        then(applyService).should(Mockito.times(1)).applyRound(10L);
        then(applyService).should(Mockito.never()).failRound(Mockito.anyLong(), Mockito.any());
        then(overlay).shouldHaveNoInteractions();
    }

    private DonationReceived donation() {
        return new DonationReceived(
                "event-1", "CHAT", "streamer-1", "user-1", "시청자", "1000", "!룰렛", Map.of()
        );
    }

    private OptionResult option(Long id, int probability, boolean losing) {
        return new OptionResult(
                id, 1L, losing ? "꽝" : "포인트", probability, losing,
                losing ? RewardType.CUSTOM : RewardType.POINT,
                losing ? ConversionMode.NONE : ConversionMode.AUTO,
                losing ? null : 100L,
                id.intValue(), NOW
        );
    }

    private RoundResult round(RouletteRoundStatus status) {
        return new RoundResult(
                10L, 7L, 1L, "event-1", "user-1", "시청자", 1L, 1, "포인트", false,
                RewardType.POINT, ConversionMode.AUTO, 100L, status, null, 42, NOW, NOW
        );
    }

}
