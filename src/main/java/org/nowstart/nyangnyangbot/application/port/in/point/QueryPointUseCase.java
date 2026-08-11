package org.nowstart.nyangnyangbot.application.port.in.point;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QueryPointUseCase {

    Page<PointSummaryResult> getList(@NotNull(message = "pageable is required") Pageable pageable);

    Page<PointSummaryResult> getByDisplayName(
            @NotNull(message = "pageable is required") Pageable pageable,
            @NotBlank(message = "displayName is required") String displayName
    );

    List<PointHistoryResult> getHistory(
            @NotBlank(message = "userId is required") String userId,
            @Positive(message = "limit must be positive") int limit
    );

    PointMeResult getMyPoint(@NotBlank(message = "userId is required") String userId);

    Optional<String> getCurrentDisplayName(@NotBlank(message = "userId is required") String userId);

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
