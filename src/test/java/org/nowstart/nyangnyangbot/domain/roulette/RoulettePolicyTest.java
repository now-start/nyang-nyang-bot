package org.nowstart.nyangnyangbot.domain.roulette;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenCode;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.ItemResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.TableResult;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteEventStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;

class RoulettePolicyTest {

    private final RoulettePolicy roulettePolicy = new RoulettePolicy();

    @Test
    void validateActivation_ShouldPass_WhenProbabilityIsCompleteAndLosingItemExists() {
        // 실행
        RouletteActivationValidation result = roulettePolicy.validateActivation(
                table("!룰렛", 1_000L),
                List.of(
                        item("당첨", 7_000, false),
                        item("꽝", 3_000, true)
                )
        );

        // 검증
        then(result.activatable()).isTrue();
        then(result.probabilityTotal()).isEqualTo(10_000);
        then(result.hasLosingItem()).isTrue();
        then(result.reasons()).isEmpty();
    }

    @Test
    void validateActivation_ShouldFail_WhenProbabilityOrLosingItemIsInvalid() {
        // 실행
        RouletteActivationValidation result = roulettePolicy.validateActivation(
                table("", 0L),
                List.of(item("당첨", 9_000, false))
        );

        // 검증
        then(result.activatable()).isFalse();
        then(result.reasons()).contains(
                "command is required",
                "pricePerRound is required",
                "probability total must be 10000",
                "losing item is required"
        );
    }

    @Test
    void validateActivation_ShouldIgnoreNullProbabilityAndZeroProbabilityLosingItem() {
        // 실행
        RouletteActivationValidation result = roulettePolicy.validateActivation(
                table("!룰렛", 1_000L),
                List.of(
                        item("당첨", null, false),
                        item("꽝", 0, true)
                )
        );

        // 검증
        then(result.activatable()).isFalse();
        then(result.probabilityTotal()).isZero();
        then(result.hasLosingItem()).isFalse();
        then(result.reasons()).contains(
                "probability total must be 10000",
                "losing item is required"
        );
    }

    @Test
    void selectItem_ShouldUseTicketAgainstCumulativeProbability() {
        // 실행
        List<ItemResult> items = List.of(
                item("A", 3_000, false),
                item("B", 7_000, true)
        );

        // 검증
        then(roulettePolicy.selectItem(items, 3_000).label()).isEqualTo("A");
        then(roulettePolicy.selectItem(items, 3_001).label()).isEqualTo("B");
    }

    @Test
    void selectItem_ShouldFallbackToLastItem_WhenTicketExceedsCumulativeProbability() {
        // 준비
        List<ItemResult> items = List.of(
                item("A", 1_000, false),
                item("B", 1_000, true)
        );

        // 실행
        ItemResult result = roulettePolicy.selectItem(items, 9_999);

        // 검증
        then(result.label()).isEqualTo("B");
    }

    @Test
    void calculateRoundCount_ShouldFloorByPriceAndRejectOverflow() {
        // 실행 및 검증
        then(roulettePolicy.calculateRoundCount(9_999L, 1_000L)).isEqualTo(9);
        then(roulettePolicy.calculateRoundCount(1_000L, 0L)).isZero();

        thenThrownBy(() -> roulettePolicy.calculateRoundCount(Long.MAX_VALUE, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("roundCount is too large");
    }

    @Test
    void containsCommand_ShouldMatchExactWhitespaceSeparatedTokenOnly() {
        // 실행 및 검증
        then(roulettePolicy.containsCommand("후원 !룰렛 갑니다", "!룰렛")).isTrue();
        then(roulettePolicy.containsCommand("후원 !룰렛갑니다", "!룰렛")).isFalse();
    }

    @Test
    void containsCommand_ShouldRejectBlankDonationTextOrCommand() {
        // 실행 및 검증
        then(roulettePolicy.containsCommand(null, "!룰렛")).isFalse();
        then(roulettePolicy.containsCommand(" ", "!룰렛")).isFalse();
        then(roulettePolicy.containsCommand("후원 !룰렛", null)).isFalse();
        then(roulettePolicy.containsCommand("후원 !룰렛", " ")).isFalse();
    }

    @Test
    void parseDonationAmount_ShouldExtractDigitsAndFallbackToZero() {
        // 실행 및 검증
        then(roulettePolicy.parseDonationAmount("1,234원")).isEqualTo(1_234L);
        then(roulettePolicy.parseDonationAmount("후원")).isZero();
        then(roulettePolicy.parseDonationAmount(null)).isZero();
        then(roulettePolicy.parseDonationAmount("999999999999999999999999")).isZero();
    }

    @Test
    void safeLimits_ShouldClampRecentRoundsAndSimulationIterations() {
        // 실행 및 검증
        then(roulettePolicy.safeRecentRoundLimit(0)).isEqualTo(1);
        then(roulettePolicy.safeRecentRoundLimit(100)).isEqualTo(50);
        then(roulettePolicy.safeSimulationIterations(0)).isEqualTo(1);
        then(roulettePolicy.safeSimulationIterations(20_000)).isEqualTo(10_000);
    }

    @Test
    void highRoundThreshold_ShouldUseDefaultWhenTableValueIsMissing() {
        // 실행 및 검증
        then(roulettePolicy.highRoundThreshold(table("!룰렛", 1_000L, null)))
                .isEqualTo(RoulettePolicy.DEFAULT_HIGH_ROUND_THRESHOLD);
        then(roulettePolicy.highRoundThreshold(table("!룰렛", 1_000L, 20))).isEqualTo(20);
    }

    @Test
    void validateTableAndItemInput_ShouldRejectInvalidBusinessFields() {
        // 실행 및 검증
        thenCode(() -> roulettePolicy.validateTableInput("기본", "!룰렛", 1_000L))
                .doesNotThrowAnyException();
        thenThrownBy(() -> roulettePolicy.validateTableInput(" ", "!룰렛", 1_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title is required");
        thenThrownBy(() -> roulettePolicy.validateItemInput(
                "호감도",
                1_000,
                RewardType.FAVORITE,
                ConversionMode.AUTO,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exchangeFavoriteValue is required for AUTO conversion");
    }

    @Test
    void validateTableInput_ShouldRejectCommandAndPriceIndependently() {
        // 실행 및 검증
        thenThrownBy(() -> roulettePolicy.validateTableInput("기본", " ", 1_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("command is required");
        thenThrownBy(() -> roulettePolicy.validateTableInput("기본", "룰렛", 1_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("command must start with !");
        thenThrownBy(() -> roulettePolicy.validateTableInput("기본", "!룰렛", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pricePerRound is required");
        thenThrownBy(() -> roulettePolicy.validateTableInput("기본", "!룰렛", -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pricePerRound is required");
    }

    @Test
    void validateItemInput_ShouldRejectEachRequiredFieldIndependently() {
        // 실행 및 검증
        thenThrownBy(() -> roulettePolicy.validateItemInput(
                " ",
                1_000,
                RewardType.FAVORITE,
                ConversionMode.MANUAL,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("label is required");
        thenThrownBy(() -> roulettePolicy.validateItemInput(
                "호감도",
                null,
                RewardType.FAVORITE,
                ConversionMode.MANUAL,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("probabilityBasisPoints is required");
        thenThrownBy(() -> roulettePolicy.validateItemInput(
                "호감도",
                -1,
                RewardType.FAVORITE,
                ConversionMode.MANUAL,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("probabilityBasisPoints is required");
        thenThrownBy(() -> roulettePolicy.validateItemInput(
                "호감도",
                1_000,
                null,
                ConversionMode.MANUAL,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("rewardType is required");
        thenThrownBy(() -> roulettePolicy.validateItemInput(
                "호감도",
                1_000,
                RewardType.FAVORITE,
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("conversionMode is required");
        thenThrownBy(() -> roulettePolicy.validateItemInput(
                "호감도",
                1_000,
                RewardType.FAVORITE,
                ConversionMode.AUTO,
                0
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exchangeFavoriteValue is required for AUTO conversion");
    }

    @Test
    void eventStatus_ShouldSummarizeRoundStatuses() {
        // 실행 및 검증
        then(roulettePolicy.eventStatus(List.of(
                () -> RouletteRoundStatus.APPLIED,
                () -> RouletteRoundStatus.APPLIED
        ))).isEqualTo(RouletteEventStatus.APPLIED);
        then(roulettePolicy.eventStatus(List.of(
                () -> RouletteRoundStatus.FAILED,
                () -> RouletteRoundStatus.FAILED
        ))).isEqualTo(RouletteEventStatus.FAILED);
        then(roulettePolicy.eventStatus(List.of(
                () -> RouletteRoundStatus.APPLIED,
                () -> RouletteRoundStatus.FAILED
        ))).isEqualTo(RouletteEventStatus.PARTIALLY_APPLIED);
        then(roulettePolicy.eventStatus(List.of(
                () -> RouletteRoundStatus.CONFIRMED
        ))).isEqualTo(RouletteEventStatus.CONFIRMED);
    }

    private TableResult table(String command, Long pricePerRound) {
        return table(command, pricePerRound, 100);
    }

    private TableResult table(String command, Long pricePerRound, Integer highRoundThreshold) {
        return new TableResult(1L, "기본", command, pricePerRound, true, 1, highRoundThreshold);
    }

    private ItemResult item(String label, Integer probabilityBasisPoints, boolean losingItem) {
        return new ItemResult(
                probabilityBasisPoints == null ? 0L : probabilityBasisPoints.longValue(),
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
