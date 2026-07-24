package org.nowstart.nyangnyangbot.application.port.out.weekly;

import java.time.Instant;
import java.util.List;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase.WeeklyChatRankView;

public interface WeeklyChatCountPort {

    void increment(Instant weekStartedAt, String userId);

    List<WeeklyChatRankView> findWeeklyRanks(Instant weekStartedAt, int limit);
}
