package org.nowstart.nyangnyangbot.domain.model;

import java.time.LocalDate;

public record WeeklyChatRankRecord(
        Long id,
        LocalDate weekStartDate,
        String userId,
        String nickName,
        Long chatCount
) {
}
