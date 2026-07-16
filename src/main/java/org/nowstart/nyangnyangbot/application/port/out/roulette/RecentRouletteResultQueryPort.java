package org.nowstart.nyangnyangbot.application.port.out.roulette;

import java.util.List;

/** Read-only recent roulette view used by message template variables. */
public interface RecentRouletteResultQueryPort {

    /** Returns up to five most recent rounds for the viewer. */
    List<RecentRound> findRecentRoundsByUserId(String userId);

    record RecentRound(Integer roundNo, String itemLabel) {
    }
}
