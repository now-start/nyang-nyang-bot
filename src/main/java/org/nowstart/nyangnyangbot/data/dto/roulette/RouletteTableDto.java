package org.nowstart.nyangnyangbot.data.dto.roulette;

import java.util.List;
import org.nowstart.nyangnyangbot.data.entity.RouletteItemEntity;
import org.nowstart.nyangnyangbot.data.entity.RouletteTableEntity;
import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.RewardType;

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
                RouletteTableEntity table,
                ValidationResponse validation,
                List<RouletteItemEntity> items
        ) {
            return new Response(
                    table.getId(),
                    table.getTitle(),
                    table.getCommand(),
                    table.getPricePerRound(),
                    table.isActive(),
                    table.getVersion(),
                    table.getHighRoundThreshold(),
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

        public static ItemResponse from(RouletteItemEntity item) {
            return new ItemResponse(
                    item.getId(),
                    item.getLabel(),
                    item.getProbabilityBasisPoints(),
                    item.isLosingItem(),
                    item.getRewardType(),
                    item.getConversionMode(),
                    item.getExchangeFavoriteValue(),
                    item.isActive(),
                    item.getDisplayOrder()
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
