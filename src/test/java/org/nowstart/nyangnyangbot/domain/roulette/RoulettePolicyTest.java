package org.nowstart.nyangnyangbot.domain.roulette;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.domain.model.RouletteItem;
import org.nowstart.nyangnyangbot.domain.model.RouletteTable;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

class RoulettePolicyTest {

    private final RoulettePolicy roulettePolicy = new RoulettePolicy();

    @Test
    void validateActivation_ShouldPass_WhenProbabilityIsCompleteAndLosingItemExists() {
        RouletteActivationValidation result = roulettePolicy.validateActivation(
                table("!룰렛", 1_000L),
                List.of(
                        item("당첨", 7_000, false),
                        item("꽝", 3_000, true)
                )
        );

        assertThat(result.activatable()).isTrue();
        assertThat(result.probabilityTotal()).isEqualTo(10_000);
        assertThat(result.hasLosingItem()).isTrue();
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void validateActivation_ShouldFail_WhenProbabilityOrLosingItemIsInvalid() {
        RouletteActivationValidation result = roulettePolicy.validateActivation(
                table("", 0L),
                List.of(item("당첨", 9_000, false))
        );

        assertThat(result.activatable()).isFalse();
        assertThat(result.reasons()).contains(
                "command is required",
                "pricePerRound is required",
                "probability total must be 10000",
                "losing item is required"
        );
    }

    @Test
    void selectItem_ShouldUseTicketAgainstCumulativeProbability() {
        List<RouletteItem> items = List.of(
                item("A", 3_000, false),
                item("B", 7_000, true)
        );

        assertThat(roulettePolicy.selectItem(items, 3_000).label()).isEqualTo("A");
        assertThat(roulettePolicy.selectItem(items, 3_001).label()).isEqualTo("B");
    }

    @Test
    void calculateRoundCount_ShouldFloorByPriceAndRejectOverflow() {
        assertThat(roulettePolicy.calculateRoundCount(9_999L, 1_000L)).isEqualTo(9);
        assertThat(roulettePolicy.calculateRoundCount(1_000L, 0L)).isZero();

        assertThatThrownBy(() -> roulettePolicy.calculateRoundCount(Long.MAX_VALUE, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("roundCount is too large");
    }

    @Test
    void containsCommand_ShouldMatchExactWhitespaceSeparatedTokenOnly() {
        assertThat(roulettePolicy.containsCommand("후원 !룰렛 갑니다", "!룰렛")).isTrue();
        assertThat(roulettePolicy.containsCommand("후원 !룰렛갑니다", "!룰렛")).isFalse();
    }

    private RouletteTable table(String command, Long pricePerRound) {
        return new RouletteTable(1L, "기본", command, pricePerRound, true, 1, 100);
    }

    private RouletteItem item(String label, Integer probabilityBasisPoints, boolean losingItem) {
        return new RouletteItem(
                1L,
                1L,
                label,
                probabilityBasisPoints,
                losingItem,
                RewardType.FAVORITE,
                ConversionMode.MANUAL,
                10,
                true,
                0
        );
    }
}
