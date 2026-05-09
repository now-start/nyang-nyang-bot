package org.nowstart.nyangnyangbot.domain.favorite;

import lombok.Builder;

@Builder
public record FavoriteLedgerEntry(
        Long id,
        String userId,
        int delta,
        int balanceAfter,
        FavoriteSourceType sourceType,
        String sourceId,
        String displayCategory,
        String publicDescription,
        String privateMemo,
        Long correctionOfLedgerId,
        String actorId,
        String idempotencyKey,
        String nickNameSnapshot
) {

    public FavoriteLedgerEntry {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (delta == 0) {
            throw new IllegalArgumentException("delta must not be zero");
        }
        if (sourceType == null) {
            throw new IllegalArgumentException("sourceType is required");
        }
    }
}
