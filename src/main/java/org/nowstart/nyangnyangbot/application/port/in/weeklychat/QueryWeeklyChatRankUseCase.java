package org.nowstart.nyangnyangbot.application.port.in.weeklychat;

import jakarta.validation.constraints.Positive;
import java.util.List;

public interface QueryWeeklyChatRankUseCase {

    /** 이번 주 채팅 횟수 순으로 사용자를 최대 {@code limit}명 반환한다. */
    List<WeeklyChatRankView> getWeeklyRanks(@Positive(message = "limit must be positive") int limit);

    record WeeklyChatRankView(
            Integer rank,
            String nickname,
            Long chatCount
    ) {
    }
}
