package org.nowstart.nyangnyangbot.application.port.out.roulette;

import java.util.List;

/** Read-only recent roulette view used by message template variables. */
public interface RecentRouletteResultQueryPort {

    int MAX_RECENT_ROUNDS = 5;

    /** Returns up to {@value #MAX_RECENT_ROUNDS} most recent rounds for the viewer. */
    List<RecentRound> findRecentRoundsByUserId(String userId);

    record RecentRound(Integer roundNo, String itemLabel) {
    }
}
