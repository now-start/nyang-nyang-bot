package org.nowstart.nyangnyangbot.application.port.out.favorite;

import org.nowstart.nyangnyangbot.domain.favorite.FavoriteLedgerEntry;

public interface SaveFavoriteLedgerPort {

    FavoriteLedgerEntry save(FavoriteLedgerEntry ledgerEntry);
}
