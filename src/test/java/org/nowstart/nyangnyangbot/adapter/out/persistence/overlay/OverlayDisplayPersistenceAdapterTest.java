package org.nowstart.nyangnyangbot.adapter.out.persistence.overlay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.nowstart.nyangnyangbot.adapter.out.persistence.donation.entity.Donation;
import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.entity.OverlayDisplayJob;
import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.repository.OverlayDisplayJobRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteRun;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRoundRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRoundRepository.DisplayRoundProjection;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRunRepository;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.springframework.data.domain.Pageable;

class OverlayDisplayPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-23T00:00:00Z");

    @Test
    void claimNextLoadsOnlyFiveRoundsWhilePreservingTotalCount() {
        OverlayDisplayJobRepository jobRepository = Mockito.mock(OverlayDisplayJobRepository.class);
        RouletteRoundRepository roundRepository = Mockito.mock(RouletteRoundRepository.class);
        RouletteRunRepository runRepository = Mockito.mock(RouletteRunRepository.class);
        OutboundContractValidator validator = Mockito.mock(OutboundContractValidator.class);
        RouletteRun run = Mockito.mock(RouletteRun.class);
        Donation donation = Mockito.mock(Donation.class);
        OverlayDisplayJob job = Mockito.mock(OverlayDisplayJob.class);
        List<DisplayRoundProjection> displayedRounds =
                List.of(round(1), round(2), round(3), round(4), round(5));
        given(run.getDonationId()).willReturn(9L);
        given(run.getDonation()).willReturn(donation);
        given(donation.getDonorDisplayName()).willReturn("후원자");
        given(job.getId()).willReturn(1L);
        given(job.getRouletteRun()).willReturn(run);
        given(job.getExpiresAt()).willReturn(NOW.plusSeconds(120));
        given(jobRepository.findClaimableForUpdate(
                Mockito.eq(NOW),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(Pageable.class)
        )).willReturn(List.of(job));
        given(roundRepository.countByRouletteRun_DonationId(9L)).willReturn(1000L);
        given(roundRepository.findDisplayRoundsByRunId(Mockito.eq(9L), Mockito.any(Pageable.class)))
                .willReturn(displayedRounds);
        given(validator.persistenceResult(Mockito.anyString(), Mockito.any()))
                .willAnswer(invocation -> invocation.getArgument(1));
        OverlayDisplayPersistenceAdapter adapter = new OverlayDisplayPersistenceAdapter(
                jobRepository, roundRepository, runRepository, validator
        );

        var result = adapter.claimNext(NOW, "claim-1", NOW.plusSeconds(30)).orElseThrow();

        assertThat(result.roundCount()).isEqualTo(1000);
        assertThat(result.rounds()).hasSize(5);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        then(roundRepository).should().findDisplayRoundsByRunId(Mockito.eq(9L), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        then(roundRepository).should(Mockito.never())
                .findByRouletteRun_DonationIdOrderByRoundNoAsc(Mockito.anyLong());
    }

    @Test
    void enqueueCreatesJobWithoutBuildingDisplayPayload() {
        OverlayDisplayJobRepository jobRepository = Mockito.mock(OverlayDisplayJobRepository.class);
        RouletteRoundRepository roundRepository = Mockito.mock(RouletteRoundRepository.class);
        RouletteRunRepository runRepository = Mockito.mock(RouletteRunRepository.class);
        OutboundContractValidator validator = Mockito.mock(OutboundContractValidator.class);
        RouletteRun run = Mockito.mock(RouletteRun.class);
        OverlayDisplayJob saved = Mockito.mock(OverlayDisplayJob.class);
        given(runRepository.findByIdForUpdate(9L)).willReturn(Optional.of(run));
        given(jobRepository.findByIdempotencyKey("roulette-run:9")).willReturn(Optional.empty());
        given(jobRepository.save(Mockito.any())).willReturn(saved);
        given(saved.getId()).willReturn(1L);
        OverlayDisplayPersistenceAdapter adapter = new OverlayDisplayPersistenceAdapter(
                jobRepository, roundRepository, runRepository, validator
        );

        adapter.enqueue(9L, "roulette-run:9", NOW.plusSeconds(120), NOW);

        then(jobRepository).should().save(Mockito.any(OverlayDisplayJob.class));
        then(roundRepository).shouldHaveNoInteractions();
        then(validator).shouldHaveNoInteractions();
    }

    @Test
    void replayReturnsOnlyCreatedJobIdWithoutBuildingDisplayPayload() {
        OverlayDisplayJobRepository jobRepository = Mockito.mock(OverlayDisplayJobRepository.class);
        RouletteRoundRepository roundRepository = Mockito.mock(RouletteRoundRepository.class);
        RouletteRunRepository runRepository = Mockito.mock(RouletteRunRepository.class);
        OutboundContractValidator validator = Mockito.mock(OutboundContractValidator.class);
        RouletteRun run = Mockito.mock(RouletteRun.class);
        OverlayDisplayJob replayOf = Mockito.mock(OverlayDisplayJob.class);
        OverlayDisplayJob saved = Mockito.mock(OverlayDisplayJob.class);
        given(runRepository.findByIdForUpdate(9L)).willReturn(Optional.of(run));
        given(jobRepository.findFirstByRouletteRun_DonationIdOrderByCreatedAtDescIdDesc(9L))
                .willReturn(Optional.of(replayOf));
        given(jobRepository.save(Mockito.any())).willReturn(saved);
        given(saved.getId()).willReturn(2L);
        OverlayDisplayPersistenceAdapter adapter = new OverlayDisplayPersistenceAdapter(
                jobRepository, roundRepository, runRepository, validator
        );

        Long displayJobId = adapter.replay(
                9L,
                "roulette-run:9:replay:claim-1",
                NOW.plusSeconds(120),
                NOW
        );

        assertThat(displayJobId).isEqualTo(2L);
        then(roundRepository).shouldHaveNoInteractions();
        then(validator).shouldHaveNoInteractions();
    }

    private DisplayRoundProjection round(int roundNo) {
        DisplayRoundProjection round = Mockito.mock(DisplayRoundProjection.class);
        given(round.getId()).willReturn((long) roundNo);
        given(round.getRoundNo()).willReturn(roundNo);
        given(round.getOptionLabel()).willReturn("포인트");
        given(round.getRewardType()).willReturn(RewardType.POINT);
        given(round.getConversionMode()).willReturn(ConversionMode.AUTO);
        given(round.getPointDelta()).willReturn(100L);
        given(round.getStatus()).willReturn(RouletteRoundStatus.APPLIED);
        return round;
    }
}
