package org.nowstart.nyangnyangbot.application.favorite;

public interface GrantFavoriteUseCase {

    FavoriteLedgerResult grant(AdjustFavoriteCommand command);
}
