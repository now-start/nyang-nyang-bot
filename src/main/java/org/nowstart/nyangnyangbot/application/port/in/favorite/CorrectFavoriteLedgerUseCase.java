package org.nowstart.nyangnyangbot.application.port.in.favorite;

import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.FavoriteLedgerResult;

public interface CorrectFavoriteLedgerUseCase {

    FavoriteLedgerResult correct(AdjustFavoriteCommand command);
}
