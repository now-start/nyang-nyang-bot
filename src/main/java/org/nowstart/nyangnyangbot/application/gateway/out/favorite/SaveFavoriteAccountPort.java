package org.nowstart.nyangnyangbot.application.gateway.out.favorite;

import org.nowstart.nyangnyangbot.domain.favorite.FavoriteAccount;

public interface SaveFavoriteAccountPort {

    FavoriteAccount save(FavoriteAccount account);
}
