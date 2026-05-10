package org.nowstart.nyangnyangbot.application.gateway.out.weekly;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.model.WeeklyChatRankRecord;
import org.nowstart.nyangnyangbot.application.dto.WeeklyChatRankDto;

public interface WeeklyChatRankPort {

    Optional<WeeklyChatRankRecord> findByWeekStartDateAndUserId(LocalDate weekStartDate, String userId);

    WeeklyChatRankRecord save(WeeklyChatRankRecord record);

    List<WeeklyChatRankDto> findWeeklyRanks(LocalDate weekStartDate, int limit);
}
