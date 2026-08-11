package org.nowstart.nyangnyangbot.application.port.out.roulette;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/** Read-only recent roulette view used by message template variables. */
public interface RecentRouletteResultQueryPort {

    int MAX_RECENT_ROUNDS = 5;

    /** Returns up to {@value #MAX_RECENT_ROUNDS} most recent rounds for the viewer. */
    List<RecentRound> findRecentRoundsByUserId(String userId);

    record RecentRound(
            @NotNull(message = "roundNo is required")
            @Positive(message = "roundNo must be positive") Integer roundNo,
            @NotBlank(message = "itemLabel is required") String itemLabel
    ) {
    }
}
