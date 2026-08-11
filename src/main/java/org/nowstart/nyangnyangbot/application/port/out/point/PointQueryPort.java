package org.nowstart.nyangnyangbot.application.port.out.point;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.point.PointSourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PointQueryPort {

    Page<PointSummaryRecord> findAll(Pageable pageable);

    Page<PointSummaryRecord> findByDisplayName(Pageable pageable, String displayName);

    Optional<PointSummaryRecord> findByUserId(String userId);

    List<PointHistoryRecord> findHistory(String userId, int limit);

    Optional<Long> findBalanceByUserId(String userId);

    long countByBalanceGreaterThan(long balance);

    record PointSummaryRecord(
            @NotBlank(message = "userId is required") String userId,
            String displayName,
            long balance
    ) {
    }

    record PointHistoryRecord(
            @Positive(message = "ledgerId must be positive") long ledgerId,
            @NotBlank(message = "userId is required") String userId,
            long delta,
            long balanceAfter,
            @NotNull(message = "sourceType is required") PointSourceType sourceType,
            @NotBlank(message = "description is required") String description,
            boolean correction,
            @NotNull(message = "createdAt is required") Instant createdAt
    ) {
    }
}
