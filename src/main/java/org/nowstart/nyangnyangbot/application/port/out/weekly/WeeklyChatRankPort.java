package org.nowstart.nyangnyangbot.application.port.out.weekly;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase.WeeklyChatRankView;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundResult;

public interface WeeklyChatRankPort {

    Optional<WeeklyChatRankRecordResult> findByWeekStartDateAndUserId(LocalDate weekStartDate, String userId);

    WeeklyChatRankRecordResult save(WeeklyChatRankRecordResult record);

    List<WeeklyChatRankView> findWeeklyRanks(LocalDate weekStartDate, int limit);

    record WeeklyChatRankRecordResult(
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive")
            Long id,
            @NotNull(message = "weekStartDate is required")
            LocalDate weekStartDate,
            @NotBlank(message = "userId is required")
            String userId,
            @NotBlank(message = "nickName is required")
            String nickName,
            @NotNull(message = "chatCount is required")
            @PositiveOrZero(message = "chatCount must not be negative")
            Long chatCount
    ) {
    }
}
