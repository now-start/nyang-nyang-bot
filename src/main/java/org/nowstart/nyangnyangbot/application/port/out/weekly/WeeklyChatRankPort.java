package org.nowstart.nyangnyangbot.application.port.out.weekly;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase.WeeklyChatRankView;

public interface WeeklyChatRankPort {

    Optional<WeeklyChatRankRecordResult> findByWeekStartDateAndUserId(LocalDate weekStartDate, String userId);

    WeeklyChatRankRecordResult save(WeeklyChatRankRecordResult record);

    List<WeeklyChatRankView> findWeeklyRanks(LocalDate weekStartDate, int limit);

    record WeeklyChatRankRecordResult(
            Long id,
            LocalDate weekStartDate,
            String userId,
            String nickName,
            Long chatCount
    ) {
    }
}
