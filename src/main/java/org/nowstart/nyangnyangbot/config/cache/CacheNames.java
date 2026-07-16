package org.nowstart.nyangnyangbot.config.cache;

import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CacheNames {

    public static final String FAVORITE_ADJUSTMENTS = "favoriteAdjustments";
    public static final String UPBO_ACTIVE_TEMPLATES = "upboActiveTemplates";
    public static final String UPBO_TEMPLATE_BY_ID = "upboTemplateById";
    public static final String ROULETTE_TABLES = "rouletteTables";
    public static final String ROULETTE_TABLE_BY_ID = "rouletteTableById";
    public static final String ROULETTE_ITEMS_BY_TABLE_ID = "rouletteItemsByTableId";
    public static final String ROULETTE_ACTIVE_ITEMS_BY_TABLE_ID = "rouletteActiveItemsByTableId";
    public static final String ROULETTE_LATEST_ACTIVE_TABLE = "rouletteLatestActiveTable";
    public static final String COMMAND_ACTIVE_BY_TRIGGER = "commandActiveByTrigger";
    public static final List<String> ALL = List.of(
            FAVORITE_ADJUSTMENTS,
            UPBO_ACTIVE_TEMPLATES,
            UPBO_TEMPLATE_BY_ID,
            ROULETTE_TABLES,
            ROULETTE_TABLE_BY_ID,
            ROULETTE_ITEMS_BY_TABLE_ID,
            ROULETTE_ACTIVE_ITEMS_BY_TABLE_ID,
            ROULETTE_LATEST_ACTIVE_TABLE,
            COMMAND_ACTIVE_BY_TRIGGER
    );

}
