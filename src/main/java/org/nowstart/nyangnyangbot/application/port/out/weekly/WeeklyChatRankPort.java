package org.nowstart.nyangnyangbot.application.port.out.weekly;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.model.WeeklyChatRankRecord;
import org.nowstart.nyangnyangbot.data.dto.WeeklyChatRankDto;

public interface WeeklyChatRankPort {

    Optional<WeeklyChatRankRecord> findByWeekStartDateAndUserId(LocalDate weekStartDate, String userId);

    WeeklyChatRankRecord save(WeeklyChatRankRecord record);

    List<WeeklyChatRankDto> findWeeklyRanks(LocalDate weekStartDate, int limit);
}
