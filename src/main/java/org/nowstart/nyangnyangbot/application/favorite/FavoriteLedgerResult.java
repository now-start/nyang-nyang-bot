package org.nowstart.nyangnyangbot.application.favorite;

public record FavoriteLedgerResult(
        String userId,
        int beforeBalance,
        int delta,
        int afterBalance,
        String history,
        boolean duplicate
) {

    public static FavoriteLedgerResult duplicate(String userId) {
        return new FavoriteLedgerResult(userId, 0, 0, 0, null, true);
    }
}
