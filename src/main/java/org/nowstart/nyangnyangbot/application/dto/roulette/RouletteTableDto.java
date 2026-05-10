package org.nowstart.nyangnyangbot.application.dto.roulette;

import java.util.List;
import org.nowstart.nyangnyangbot.application.model.RouletteItem;
import org.nowstart.nyangnyangbot.application.model.RouletteTable;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

public class RouletteTableDto {

    public record CreateRequest(
            String title,
            String command,
            Long pricePerRound,
            Integer highRoundThreshold
    ) {
    }

    public record ItemRequest(
            String label,
            Integer probabilityBasisPoints,
            Boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            Integer displayOrder
    ) {
    }

    public record Response(
            Long id,
            String title,
            String command,
            Long pricePerRound,
            Boolean active,
            Integer version,
            Integer highRoundThreshold,
            ValidationResponse validation,
            List<ItemResponse> items
    ) {

        public static Response from(
                RouletteTable table,
                ValidationResponse validation,
                List<RouletteItem> items
        ) {
            return new Response(
                    table.id(),
                    table.title(),
                    table.command(),
                    table.pricePerRound(),
                    table.active(),
                    table.version(),
                    table.highRoundThreshold(),
                    validation,
                    items.stream().map(ItemResponse::from).toList()
            );
        }
    }

    public record ItemResponse(
            Long id,
            String label,
            Integer probabilityBasisPoints,
            Boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            Boolean active,
            Integer displayOrder
    ) {

        public static ItemResponse from(RouletteItem item) {
            return new ItemResponse(
                    item.id(),
                    item.label(),
                    item.probabilityBasisPoints(),
                    item.losingItem(),
                    item.rewardType(),
                    item.conversionMode(),
                    item.exchangeFavoriteValue(),
                    item.active(),
                    item.displayOrder()
            );
        }
    }

    public record ValidationResponse(
            Boolean activatable,
            List<String> reasons,
            Integer probabilityTotal,
            Boolean hasLosingItem
    ) {
    }

    public record SimulationResponse(
            Integer iterations,
            List<SimulationItem> items
    ) {
    }

    public record SimulationItem(
            String label,
            Integer count,
            Double ratio
    ) {
    }
}
