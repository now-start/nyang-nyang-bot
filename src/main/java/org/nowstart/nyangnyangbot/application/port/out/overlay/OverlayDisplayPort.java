package org.nowstart.nyangnyangbot.application.port.out.overlay;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundResult;

public interface OverlayDisplayPort {

    void enqueueRouletteEvent(Long rouletteEventId, LocalDateTime expiresAt);

    DisplayEventResult replayRouletteEvent(Long rouletteEventId, LocalDateTime expiresAt);

    void markPendingExpiredBefore(LocalDateTime current);

    Optional<DisplayEventResult> claimNextPending(LocalDateTime current);

    void markDisplayed(Long displayEventId, LocalDateTime displayedAt);

    record DisplayEventResult(
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive")
            Long id,
            @NotNull(message = "rouletteEventId is required")
            @Positive(message = "rouletteEventId must be positive")
            Long rouletteEventId,
            String nickName,
            @NotNull(message = "roundCount is required")
            @Positive(message = "roundCount must be positive")
            Integer roundCount,
            @NotNull(message = "expiresAt is required")
            LocalDateTime expiresAt,
            @NotNull(message = "rounds is required")
            List<@Valid @NotNull(message = "round is required") DisplayRoundResult> rounds
    ) {
    }

    record DisplayRoundResult(
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive")
            Long id,
            @NotNull(message = "roundNo is required")
            @Positive(message = "roundNo must be positive")
            Integer roundNo,
            @NotBlank(message = "itemLabel is required")
            String itemLabel,
            boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            @NotNull(message = "status is required")
            RouletteRoundStatus status,
            Long ledgerId,
            Long userUpboId,
            String failureReason
    ) {
    }
}
