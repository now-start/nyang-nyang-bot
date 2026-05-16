package org.nowstart.nyangnyangbot.application.port.out.favorite;

import org.nowstart.nyangnyangbot.domain.favorite.FavoriteAccount;

public interface SaveFavoriteAccountPort {

    FavoriteAccount save(FavoriteAccount account);
}
