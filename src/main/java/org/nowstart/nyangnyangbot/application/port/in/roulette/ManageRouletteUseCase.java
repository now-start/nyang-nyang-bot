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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ManageRouletteUseCase {

    RouletteConfigResult createConfig(@Valid @NotNull CreateRouletteConfigCommand command);

    RouletteOptionResult addOption(@Valid @NotNull AddRouletteOptionCommand command);

    Page<RouletteConfigSummaryResult> getConfigs(Pageable pageable);

    RouletteConfigResult getConfig(Long configId);

    RouletteConfigResult activateConfig(Long configId);

    RouletteConfigResult archiveConfig(Long configId);

    RouletteSimulationResult simulate(Long configId, int iterations);

    record CreateRouletteConfigCommand(
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 20) String triggerToken,
            @NotNull @Positive Long pricePerRound,
            @Positive Integer highRoundThreshold
    ) {
    }

    record AddRouletteOptionCommand(
            @NotNull @Positive Long configId,
            @NotBlank @Size(max = 100) String label,
            @NotNull @Min(0) @Max(10_000) Integer probabilityBasisPoints,
            Boolean losing,
            String rewardType,
            String conversionMode,
            Long pointDelta,
            @PositiveOrZero Integer displayOrder
    ) {
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
