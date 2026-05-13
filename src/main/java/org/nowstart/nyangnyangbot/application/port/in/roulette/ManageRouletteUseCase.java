package org.nowstart.nyangnyangbot.application.port.in.roulette;

import java.util.List;

public interface ManageRouletteUseCase {

    RouletteTableResult createTable(String title, String command, Long pricePerRound, Integer highRoundThreshold);

    RouletteItemResult addItem(
            Long tableId,
            String label,
            Integer probabilityBasisPoints,
            Boolean losingItem,
            String rewardType,
            String conversionMode,
            Integer exchangeFavoriteValue,
            Integer displayOrder
    );

    List<RouletteTableResult> getTables();

    RouletteValidationResult validateTable(Long tableId);

    RouletteTableResult activateTable(Long tableId);

    RouletteTableResult deactivateTable(Long tableId);

    RouletteSimulationResult simulate(Long tableId, int iterations);

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
