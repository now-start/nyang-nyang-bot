package org.nowstart.nyangnyangbot.domain.roulette;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.ConfigResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.OptionResult;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteConfigStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteProcessingStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;

class RoulettePolicyTest {

    private final RoulettePolicy policy = new RoulettePolicy();

    @Test
    void activationRequiresExactProbabilityAndPositiveLosingOption() {
        var result = policy.validateActivation(config(), List.of(
                option(1L, 7_000, false),
                option(2L, 3_000, true)
        ));

        assertThat(result.activatable()).isTrue();
        assertThat(result.probabilityTotal()).isEqualTo(10_000);
        assertThat(result.hasLosingOption()).isTrue();
    }

    @Test
    void activationRejectsMissingLosingOption() {
        var result = policy.validateActivation(config(), List.of(option(1L, 10_000, false)));

        assertThat(result.activatable()).isFalse();
        assertThat(result.reasons()).contains("losing option is required");
    }

    @Test
    void selectOptionUsesClosedTicketRanges() {
        List<OptionResult> options = List.of(option(1L, 2_500, false), option(2L, 7_500, true));

        assertThat(policy.selectOption(options, 2_500).id()).isEqualTo(1L);
        assertThat(policy.selectOption(options, 2_501).id()).isEqualTo(2L);
    }

    @Test
    void autoConversionRequiresNonZeroPointDelta() {
        assertThatThrownBy(() -> policy.validateOptionInput(
                "포인트",
                5_000,
                false,
                RewardType.POINT,
                ConversionMode.AUTO,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pointDelta is required for AUTO conversion");
    }

    @Test
    void losingOptionMustUseNoneConversion() {
        assertThatThrownBy(() -> policy.validateOptionInput(
                "꽝",
                5_000,
                true,
                RewardType.CUSTOM,
                ConversionMode.MANUAL,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("losing option must use NONE conversion");
    }

    @Test
    void triggerTokenMustStartWithExclamationMark() {
        assertThatThrownBy(() -> policy.validateConfigInput("기본", "룰렛", 1_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("triggerToken must start with !");
    }

    @Test
    void roundCountRejectsOneMoreThanTheHardOperationalLimit() {
        assertThat(policy.calculateRoundCount(RoulettePolicy.MAX_ROUNDS_PER_DONATION, 1L))
                .isEqualTo(RoulettePolicy.MAX_ROUNDS_PER_DONATION);
        assertThatThrownBy(() -> policy.calculateRoundCount(
                RoulettePolicy.MAX_ROUNDS_PER_DONATION + 1L,
                1L
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("roundCount exceeds maximum " + RoulettePolicy.MAX_ROUNDS_PER_DONATION);
    }

    @Test
    void processingStatusIsDerivedFromRoundStates() {
        assertThat(policy.processingStatus(List.of(RouletteRoundStatus.APPLIED, RouletteRoundStatus.APPLIED)))
                .isEqualTo(RouletteProcessingStatus.APPLIED);
        assertThat(policy.processingStatus(List.of(RouletteRoundStatus.APPLIED, RouletteRoundStatus.FAILED)))
                .isEqualTo(RouletteProcessingStatus.PARTIALLY_APPLIED);
    }

    private ConfigResult config() {
        Instant now = Instant.parse("2026-07-23T00:00:00Z");
        return new ConfigResult(
                1L,
                "기본",
                "!룰렛",
                1_000L,
                100,
                RouletteConfigStatus.DRAFT,
                now,
                now
        );
    }

    private OptionResult option(Long id, int probability, boolean losing) {
        return new OptionResult(
                id,
                1L,
                losing ? "꽝" : "포인트",
                probability,
                losing,
                losing ? RewardType.CUSTOM : RewardType.POINT,
                losing ? ConversionMode.NONE : ConversionMode.AUTO,
                losing ? null : 100L,
                id.intValue(),
                Instant.parse("2026-07-23T00:00:00Z")
        );
    }
}
