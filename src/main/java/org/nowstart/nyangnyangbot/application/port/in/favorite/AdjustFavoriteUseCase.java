package org.nowstart.nyangnyangbot.application.port.in.favorite;

import lombok.Builder;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;

public interface AdjustFavoriteUseCase {

    FavoriteLedgerResult adjust(AdjustFavoriteCommand command);

    @Builder
    record AdjustFavoriteCommand(
            String userId,
            String nickName,
            int delta,
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
