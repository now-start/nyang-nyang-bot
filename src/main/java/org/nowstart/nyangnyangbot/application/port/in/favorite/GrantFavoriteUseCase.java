package org.nowstart.nyangnyangbot.application.port.in.favorite;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.FavoriteLedgerResult;

public interface GrantFavoriteUseCase {

    FavoriteLedgerResult grant(
            @Valid @NotNull(message = "command is required") AdjustFavoriteCommand command
    );
}
