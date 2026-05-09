package org.nowstart.nyangnyangbot.application.favorite;

public interface CorrectFavoriteLedgerUseCase {

    FavoriteLedgerResult correct(AdjustFavoriteCommand command);
}
