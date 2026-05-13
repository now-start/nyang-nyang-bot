package org.nowstart.nyangnyangbot.application.port.out.favorite;

import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteAccount;

public interface LoadFavoriteAccountPort {

    Optional<FavoriteAccount> loadForUpdate(String userId);
}
