package org.nowstart.nyangnyangbot.application.port.in.favorite;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;

public interface AdjustFavoriteUseCase {

    FavoriteLedgerResult adjust(AdjustFavoriteCommand command);

    @Builder
    record AdjustFavoriteCommand(
            @NotBlank(message = "userId is required")
            String userId,
            String nickName,
            int delta,
            @NotNull(message = "sourceType is required")
            FavoriteSourceType sourceType,
            String sourceId,
            String displayCategory,
            String publicDescription,
            String privateMemo,
            Long correctionOfLedgerId,
            String actorId,
            String idempotencyKey,
            boolean allowNegativeBalance,
            boolean createIfMissing
    ) {

        @AssertTrue(message = "delta must not be zero")
        public boolean isDeltaNonZero() {
            return delta != 0;
        }
    }

    record FavoriteLedgerResult(
            String userId,
            int beforeBalance,
            int delta,
            int afterBalance,
            String history,
            boolean duplicate,
            Long ledgerId
    ) {

        public static FavoriteLedgerResult duplicate(String userId) {
            return new FavoriteLedgerResult(userId, 0, 0, 0, null, true, null);
        }
    }
}
