package org.nowstart.nyangnyangbot.application.port.in.point;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QueryPointUseCase {

    Page<PointSummaryResult> getList(Pageable pageable);

    Page<PointSummaryResult> getByDisplayName(Pageable pageable, String displayName);

    List<PointHistoryResult> getHistory(String userId, int limit);

    PointMeResult getMyPoint(String userId);

    Optional<String> getCurrentDisplayName(String userId);

    record PointSummaryResult(String userId, String displayName, long point) {
    }

    record PointHistoryResult(
            long ledgerId,
            String userId,
            long delta,
            long balanceAfter,
            String sourceType,
            String description,
            boolean correction,
            Instant createdAt
    ) {
    }

    record PointMeResult(
            String userId,
            String displayName,
            long point,
            long rank
    ) {
    }
}
