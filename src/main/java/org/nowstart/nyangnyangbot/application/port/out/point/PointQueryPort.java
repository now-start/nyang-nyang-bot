package org.nowstart.nyangnyangbot.application.port.out.point;

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

    record PointSummaryRecord(String userId, String displayName, long balance) {
    }

    record PointHistoryRecord(
            long ledgerId,
            String userId,
            long delta,
            long balanceAfter,
            PointSourceType sourceType,
            String description,
            boolean correction,
            Instant createdAt
    ) {
    }
}
