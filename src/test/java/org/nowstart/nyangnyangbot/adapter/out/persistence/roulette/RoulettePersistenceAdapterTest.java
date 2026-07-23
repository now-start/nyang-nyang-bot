package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.nowstart.nyangnyangbot.adapter.out.persistence.donation.repository.DonationRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteConfig;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteOption;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteConfigRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteOptionRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRoundRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRoundRepository.RecentRoundProjection;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRoundRepository.RunRoundSummaryProjection;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRunRepository;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteConfigStatus;
import org.springframework.data.domain.Pageable;

class RoulettePersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-23T00:00:00Z");

    @Test
    void activationArchivesAndFlushesExistingActiveBeforeActivatingTarget() {
        RouletteConfigRepository configRepository = Mockito.mock(RouletteConfigRepository.class);
        RouletteOptionRepository optionRepository = Mockito.mock(RouletteOptionRepository.class);
        OutboundContractValidator validator = passThroughValidator();
        RoulettePersistenceAdapter adapter = new RoulettePersistenceAdapter(
                configRepository,
                optionRepository,
                Mockito.mock(RouletteRunRepository.class),
                Mockito.mock(RouletteRoundRepository.class),
                Mockito.mock(DonationRepository.class),
                validator
        );
        RouletteConfig active = config(1L, RouletteConfigStatus.ACTIVE);
        RouletteConfig target = config(2L, RouletteConfigStatus.DRAFT);
        given(configRepository.findByIdForUpdate(2L)).willReturn(Optional.of(target));
        given(optionRepository.findByRouletteConfig_IdOrderByDisplayOrderAscIdAsc(2L)).willReturn(List.of(
                option(target, 1L, 7_000, false),
                option(target, 2L, 3_000, true)
        ));
        given(configRepository.findByStatusForUpdate(RouletteConfigStatus.ACTIVE)).willReturn(List.of(active));
        given(configRepository.saveAndFlush(target)).willReturn(target);

        var result = adapter.activateConfig(2L, NOW.plusSeconds(1));

        InOrder order = inOrder(configRepository);
        order.verify(configRepository).findByStatusForUpdate(RouletteConfigStatus.ACTIVE);
        order.verify(configRepository).flush();
        order.verify(configRepository).saveAndFlush(target);
        assertThat(active.getStatus()).isEqualTo(RouletteConfigStatus.ARCHIVED);
        assertThat(result.status()).isEqualTo(RouletteConfigStatus.ACTIVE);
    }

    @Test
    void recentRounds_UsesBoundedProjectionQueryInsteadOfLoadingUserHistory() {
        RouletteRoundRepository roundRepository = Mockito.mock(RouletteRoundRepository.class);
        RecentRoundProjection projection = Mockito.mock(RecentRoundProjection.class);
        given(projection.getRoundNo()).willReturn(3);
        given(projection.getItemLabel()).willReturn("포인트");
        given(roundRepository.findRecentByUserId(Mockito.eq("user-1"), Mockito.any(Pageable.class)))
                .willReturn(List.of(projection));
        RoulettePersistenceAdapter adapter = new RoulettePersistenceAdapter(
                Mockito.mock(RouletteConfigRepository.class),
                Mockito.mock(RouletteOptionRepository.class),
                Mockito.mock(RouletteRunRepository.class),
                roundRepository,
                Mockito.mock(DonationRepository.class),
                passThroughValidator()
        );

        var result = adapter.findRecentRoundsByUserId("user-1");

        org.mockito.ArgumentCaptor<Pageable> pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        Mockito.verify(roundRepository).findRecentByUserId(Mockito.eq("user-1"), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(result).singleElement().satisfies(round -> {
            assertThat(round.roundNo()).isEqualTo(3);
            assertThat(round.itemLabel()).isEqualTo("포인트");
        });
        Mockito.verify(roundRepository, Mockito.never())
                .findByRouletteRun_Donation_DonorUserAccount_UserIdOrderByCreatedAtDescIdDesc(Mockito.anyString());
    }

    @Test
    void summarizeRuns_UsesOneGroupedProjectionQuery() {
        RouletteRoundRepository roundRepository = Mockito.mock(RouletteRoundRepository.class);
        RunRoundSummaryProjection projection = Mockito.mock(RunRoundSummaryProjection.class);
        given(projection.getRunId()).willReturn(7L);
        given(projection.getRoundCount()).willReturn(1000L);
        given(projection.getAppliedCount()).willReturn(999L);
        given(projection.getFailedCount()).willReturn(1L);
        given(roundRepository.summarizeRuns(List.of(7L))).willReturn(List.of(projection));
        RoulettePersistenceAdapter adapter = new RoulettePersistenceAdapter(
                Mockito.mock(RouletteConfigRepository.class),
                Mockito.mock(RouletteOptionRepository.class),
                Mockito.mock(RouletteRunRepository.class),
                roundRepository,
                Mockito.mock(DonationRepository.class),
                passThroughValidator()
        );

        var result = adapter.summarizeRuns(List.of(7L));

        assertThat(result).singleElement().satisfies(summary -> {
            assertThat(summary.runId()).isEqualTo(7L);
            assertThat(summary.roundCount()).isEqualTo(1000L);
            assertThat(summary.appliedCount()).isEqualTo(999L);
            assertThat(summary.failedCount()).isEqualTo(1L);
        });
    }

    private RouletteConfig config(Long id, RouletteConfigStatus status) {
        return RouletteConfig.builder()
                .id(id)
                .title("기본")
                .triggerToken("!룰렛")
                .pricePerRound(1_000L)
                .highRoundThreshold(100)
                .status(status)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }

    private RouletteOption option(RouletteConfig config, Long id, int probability, boolean losing) {
        return RouletteOption.builder()
                .id(id)
                .rouletteConfig(config)
                .label(losing ? "꽝" : "포인트")
                .probabilityBasisPoints(probability)
                .losing(losing)
                .rewardType(losing ? RewardType.CUSTOM : RewardType.POINT)
                .conversionMode(losing ? ConversionMode.NONE : ConversionMode.AUTO)
                .pointDelta(losing ? null : 100L)
                .displayOrder(id.intValue())
                .createdAt(NOW)
                .build();
    }

    private OutboundContractValidator passThroughValidator() {
        OutboundContractValidator validator = Mockito.mock(OutboundContractValidator.class);
        given(validator.persistenceResult(anyString(), any())).willAnswer(invocation -> invocation.getArgument(1));
        return validator;
    }
}
