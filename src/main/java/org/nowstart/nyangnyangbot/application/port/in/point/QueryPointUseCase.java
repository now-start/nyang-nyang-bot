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

    /** 요청한 페이지 및 정렬 조건에 따라 포인트 요약을 반환한다. */
    Page<PointSummaryResult> getList(@NotNull(message = "pageable is required") Pageable pageable);

    /** 표시 이름이 검색어와 일치하는 포인트 요약을 반환한다. */
    Page<PointSummaryResult> getByDisplayName(
            @NotNull(message = "pageable is required") Pageable pageable,
            @NotBlank(message = "displayName is required") String displayName
    );

    /** 사용자의 최근 원장 항목을 최대 {@code limit}개 반환한다. */
    List<PointHistoryResult> getHistory(
            @NotBlank(message = "userId is required") String userId,
            @Positive(message = "limit must be positive") int limit
    );

    /** 요청한 사용자의 포인트 잔액과 순위를 반환한다. */
    PointMeResult getMyPoint(@NotBlank(message = "userId is required") String userId);

    /** 사용자의 현재 표시 이름을 반환하며, 사용자를 찾을 수 없으면 빈 값을 반환한다. */
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
