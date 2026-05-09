package org.nowstart.nyangnyangbot.application.favorite;

public interface AdjustFavoriteUseCase {

    FavoriteLedgerResult adjust(AdjustFavoriteCommand command);
}
