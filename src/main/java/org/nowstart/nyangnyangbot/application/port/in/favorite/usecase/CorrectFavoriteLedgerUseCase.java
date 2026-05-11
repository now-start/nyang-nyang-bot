package org.nowstart.nyangnyangbot.application.port.in.favorite.usecase;

import org.nowstart.nyangnyangbot.application.port.in.favorite.dto.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.dto.FavoriteLedgerResult;

public interface CorrectFavoriteLedgerUseCase {

    FavoriteLedgerResult correct(AdjustFavoriteCommand command);
}
