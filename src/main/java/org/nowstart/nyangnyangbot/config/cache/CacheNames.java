package org.nowstart.nyangnyangbot.config.cache;

import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CacheNames {

    public static final String POINT_ADJUSTMENT_PRESETS = "pointAdjustmentPresets";
    public static final String COMMAND_ACTIVE_BY_TRIGGER = "commandActiveByTrigger";
    public static final List<String> ALL = List.of(
            POINT_ADJUSTMENT_PRESETS,
            COMMAND_ACTIVE_BY_TRIGGER
    );

}
