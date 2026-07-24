package org.nowstart.nyangnyangbot.application.port.in.roulette;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.nowstart.nyangnyangbot.domain.chat.CommandTrigger;
import org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ManageRouletteUseCase {

    int DEFAULT_SIMULATION_ITERATIONS = RoulettePolicy.DEFAULT_SIMULATION_ITERATIONS;
    int MIN_TRIGGER_LENGTH = CommandTrigger.MIN_LENGTH;
    int MAX_TRIGGER_LENGTH = CommandTrigger.MAX_LENGTH;
    String TRIGGER_LENGTH_MESSAGE = CommandTrigger.LENGTH_MESSAGE;

    RouletteConfigResult createConfig(@Valid @NotNull CreateRouletteConfigCommand command);

    RouletteOptionResult addOption(@Valid @NotNull AddRouletteOptionCommand command);

    Page<RouletteConfigSummaryResult> getConfigs(Pageable pageable);

    RouletteConfigResult getConfig(Long configId);

    RouletteConfigResult activateConfig(Long configId);

    RouletteConfigResult archiveConfig(Long configId);

    RouletteSimulationResult simulate(Long configId, int iterations);

    record CreateRouletteConfigCommand(
            @NotBlank @Size(max = 100) String title,
            @NotBlank
            @Size(min = MIN_TRIGGER_LENGTH, max = MAX_TRIGGER_LENGTH, message = TRIGGER_LENGTH_MESSAGE)
            String triggerToken,
            @NotNull @Positive Long pricePerRound,
            @Positive Integer highRoundThreshold
    ) {
    }

    record AddRouletteOptionCommand(
            @NotNull @Positive Long configId,
            @NotBlank @Size(max = 100) String label,
            @NotNull @Min(0) @Max(RoulettePolicy.TOTAL_PROBABILITY) Integer probabilityBasisPoints,
            Boolean losing,
            String rewardType,
            String conversionMode,
            Long pointDelta,
            @PositiveOrZero Integer displayOrder
    ) {
        public int displayOrderOrDefault() {
            return displayOrder == null ? 0 : displayOrder;
        }
    }

    record RouletteConfigResult(
            Long id,
            String title,
            String triggerToken,
            Long pricePerRound,
            String status,
            Integer highRoundThreshold,
            RouletteValidationResult validation,
            List<RouletteOptionResult> options,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    record RouletteOptionResult(
            Long id,
            String label,
            Integer probabilityBasisPoints,
            Boolean losing,
            String rewardType,
            String conversionMode,
            Long pointDelta,
            Integer displayOrder
    ) {
    }

    record RouletteConfigSummaryResult(
            Long id,
            String title,
            String triggerToken,
            Long pricePerRound,
            String status,
            Instant createdAt
    ) {
    }

    record RouletteValidationResult(
            Boolean activatable,
            List<String> reasons,
            Integer probabilityTotal,
            Boolean hasLosingOption
    ) {
    }

    record RouletteSimulationResult(Integer iterations, List<Entry> options) {

        public record Entry(String label, Integer count, Double ratio) {
        }
    }
}
