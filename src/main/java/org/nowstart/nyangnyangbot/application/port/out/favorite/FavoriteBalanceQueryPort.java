package org.nowstart.nyangnyangbot.application.port.out.favorite;

import java.util.Optional;

/** Read-only favorite balance view used by message template variables. */
public interface FavoriteBalanceQueryPort {

    Optional<Integer> findBalanceByUserId(String userId);
}
