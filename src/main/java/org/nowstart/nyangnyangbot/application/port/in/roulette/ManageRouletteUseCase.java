package org.nowstart.nyangnyangbot.application.port.in.roulette;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public interface ManageRouletteUseCase {

    RouletteTableResult createTable(CreateRouletteTableCommand command);

    RouletteItemResult addItem(AddRouletteItemCommand command);

    List<RouletteTableResult> getTables();

    RouletteValidationResult validateTable(Long tableId);

    RouletteTableResult activateTable(Long tableId);

    RouletteTableResult deactivateTable(Long tableId);

    RouletteSimulationResult simulate(Long tableId, int iterations);

    record CreateRouletteTableCommand(
            @NotBlank(message = "title is required")
            @Size(max = 255, message = "title length must be 255 or less")
            String title,
            @NotBlank(message = "command is required")
            @Size(max = 255, message = "command length must be 255 or less")
            String command,
            @NotNull(message = "pricePerRound is required")
            @Positive(message = "pricePerRound must be positive")
            Long pricePerRound,
            @Positive(message = "highRoundThreshold must be positive")
            Integer highRoundThreshold
    ) {
    }

    record AddRouletteItemCommand(
            @NotNull(message = "tableId is required")
            @Positive(message = "tableId must be positive")
            Long tableId,
            @NotBlank(message = "label is required")
            @Size(max = 255, message = "label length must be 255 or less")
            String label,
            @NotNull(message = "probabilityBasisPoints is required")
            @Min(value = 0, message = "probabilityBasisPoints must be between 0 and 10000")
            @Max(value = 10_000, message = "probabilityBasisPoints must be between 0 and 10000")
            Integer probabilityBasisPoints,
            Boolean losingItem,
            String rewardType,
            String conversionMode,
            Integer exchangeFavoriteValue,
            @PositiveOrZero(message = "displayOrder must not be negative")
            Integer displayOrder
    ) {
    }

    record RouletteTableResult(
            Long id,
            String title,
            String command,
            Long pricePerRound,
            Boolean active,
            Integer version,
            Integer highRoundThreshold,
            RouletteValidationResult validation,
            List<RouletteItemResult> items
    ) {
    }

    record RouletteItemResult(
            Long id,
            String label,
            Integer probabilityBasisPoints,
            Boolean losingItem,
            String rewardType,
            String conversionMode,
            Integer exchangeFavoriteValue,
            Boolean active,
            Integer displayOrder
    ) {
    }

    record RouletteValidationResult(
            Boolean activatable,
            List<String> reasons,
            Integer probabilityTotal,
            Boolean hasLosingItem
    ) {
    }

    record RouletteSimulationResult(
            Integer iterations,
            List<Entry> items
    ) {

        public record Entry(
                String label,
                Integer count,
                Double ratio
        ) {
        }
    }
}
