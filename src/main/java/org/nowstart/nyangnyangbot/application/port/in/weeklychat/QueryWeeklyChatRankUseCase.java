package org.nowstart.nyangnyangbot.application.port.in.weeklychat;

import jakarta.validation.constraints.Positive;
import java.util.List;

public interface QueryWeeklyChatRankUseCase {

    List<WeeklyChatRankView> getWeeklyRanks(@Positive(message = "limit must be positive") int limit);

    record WeeklyChatRankView(
            Integer rank,
            String nickname,
            Long chatCount
    ) {
    }
}
