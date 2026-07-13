package org.nowstart.nyangnyangbot.application.port.out.favorite;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FavoriteQueryPort {

    Page<SummaryResult> findAll(Pageable pageable);

    Page<SummaryResult> findByNickNameContains(Pageable pageable, String nickName);

    Optional<SummaryResult> findById(String userId);

    SummaryResult getOrCreate(String userId, String nickName);

    void updateNickName(String userId, String nickName);

    List<HistoryResult> findHistory(String userId, int limit);

    long countHistory(String userId);

    long countHistoryAfter(String userId, LocalDateTime createDate);

    long countByFavoriteGreaterThan(Integer favorite);

    record SummaryResult(
            @NotBlank(message = "userId is required")
            String userId,
            String nickName,
            @NotNull(message = "favorite is required")
            Integer favorite
    ) {
    }

    record HistoryResult(
            @NotNull(groups = OutboundResult.class, message = "ledgerId is required")
            @Positive(groups = OutboundResult.class, message = "ledgerId must be positive")
            Long ledgerId,
            String channelId,
            String nickNameSnapshot,
            @NotNull(message = "delta is required")
            Integer delta,
            @NotNull(message = "balanceAfter is required")
            Integer balanceAfter,
            @NotNull(message = "sourceType is required")
            FavoriteSourceType sourceType,
            String displayCategory,
            @NotBlank(message = "publicDescription is required")
            String publicDescription,
            boolean correction,
            Integer favorite,
            String history,
            LocalDateTime createdAt
    ) {
    }
}
