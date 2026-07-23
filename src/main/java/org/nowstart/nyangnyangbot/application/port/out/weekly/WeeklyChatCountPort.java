package org.nowstart.nyangnyangbot.application.port.out.weekly;

import java.time.LocalDate;
import java.util.List;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase.WeeklyChatRankView;

public interface WeeklyChatCountPort {

    void increment(LocalDate weekStartDate, String userId);

    List<WeeklyChatRankView> findWeeklyRanks(LocalDate weekStartDate, int limit);
}
