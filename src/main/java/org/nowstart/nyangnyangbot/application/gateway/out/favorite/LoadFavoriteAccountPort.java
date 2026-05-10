package org.nowstart.nyangnyangbot.application.gateway.out.favorite;

import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteAccount;

public interface LoadFavoriteAccountPort {

    Optional<FavoriteAccount> loadForUpdate(String userId);
}
