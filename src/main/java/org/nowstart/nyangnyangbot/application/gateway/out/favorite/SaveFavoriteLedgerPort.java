package org.nowstart.nyangnyangbot.application.gateway.out.favorite;

import org.nowstart.nyangnyangbot.domain.favorite.FavoriteLedgerEntry;

public interface SaveFavoriteLedgerPort {

    FavoriteLedgerEntry save(FavoriteLedgerEntry ledgerEntry);
}
