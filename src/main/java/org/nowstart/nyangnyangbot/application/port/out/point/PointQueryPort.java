package org.nowstart.nyangnyangbot.application.port.out.point;

import jakarta.validation.Valid;
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

    /** 전달된 페이지 조건에 따라 사용자 포인트 요약을 반환한다. */
    Page<@Valid PointSummaryRecord> findAll(Pageable pageable);

    /** 표시 이름 검색어와 일치하는 포인트 요약을 반환한다. */
    Page<@Valid PointSummaryRecord> findByDisplayName(Pageable pageable, String displayName);

    /** 등록된 사용자의 포인트 요약을 반환한다. */
    Optional<@Valid PointSummaryRecord> findByUserId(String userId);

    /** 사용자의 최근 원장 항목을 최대 {@code limit}개 반환한다. */
    List<@Valid PointHistoryRecord> findHistory(String userId, int limit);

    /** 등록된 사용자의 잔액을 반환하며, 원장 항목이 없으면 0을 반환한다. */
    Optional<Long> findBalanceByUserId(String userId);

    /** 잔액이 전달된 값보다 큰 사용자 수를 반환한다. */
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
