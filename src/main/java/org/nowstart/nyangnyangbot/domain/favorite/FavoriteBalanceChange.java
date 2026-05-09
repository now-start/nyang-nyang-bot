package org.nowstart.nyangnyangbot.domain.favorite;

public record FavoriteBalanceChange(
        int beforeBalance,
        int delta,
        int afterBalance
) {
}
