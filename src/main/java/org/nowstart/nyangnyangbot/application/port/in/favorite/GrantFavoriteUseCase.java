package org.nowstart.nyangnyangbot.application.port.in.favorite;

import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.FavoriteLedgerResult;

public interface GrantFavoriteUseCase {

    FavoriteLedgerResult grant(AdjustFavoriteCommand command);
}
