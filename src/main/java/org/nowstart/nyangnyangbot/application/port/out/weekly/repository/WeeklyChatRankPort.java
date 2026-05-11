package org.nowstart.nyangnyangbot.application.port.out.weekly.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.dto.WeeklyChatRankView;
import org.nowstart.nyangnyangbot.domain.model.WeeklyChatRankRecord;

public interface WeeklyChatRankPort {

    Optional<WeeklyChatRankRecord> findByWeekStartDateAndUserId(LocalDate weekStartDate, String userId);

    WeeklyChatRankRecord save(WeeklyChatRankRecord record);

    List<WeeklyChatRankView> findWeeklyRanks(LocalDate weekStartDate, int limit);
}
