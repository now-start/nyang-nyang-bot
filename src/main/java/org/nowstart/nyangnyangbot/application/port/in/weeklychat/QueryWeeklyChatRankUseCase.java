package org.nowstart.nyangnyangbot.application.port.in.weeklychat;

import java.util.List;

public interface QueryWeeklyChatRankUseCase {

    List<WeeklyChatRankView> getWeeklyRanks(int limit);

    record WeeklyChatRankView(
            Integer rank,
            String nickname,
            Long chatCount
    ) {
    }
}
