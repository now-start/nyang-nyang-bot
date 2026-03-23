package org.nowstart.nyangnyangbot.data.entity;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;

class FavoriteEntityStructureTest {

    @Test
    void setFavorite_ShouldSyncTotalFavorite() {
        FavoriteEntity favoriteEntity = FavoriteEntity.builder().userId("user1").build();

        favoriteEntity.setFavorite(42);

        then(favoriteEntity.getFavorite()).isEqualTo(42);
        then(favoriteEntity.getTotalFavorite()).isEqualTo(42);
        then(favoriteEntity.safeFavorite()).isEqualTo(42);
    }

    @Test
    void setTotalFavorite_ShouldSyncFavorite() {
        FavoriteEntity favoriteEntity = FavoriteEntity.builder().userId("user1").build();

        favoriteEntity.setTotalFavorite(87);

        then(favoriteEntity.getFavorite()).isEqualTo(87);
        then(favoriteEntity.getTotalFavorite()).isEqualTo(87);
        then(favoriteEntity.safeFavorite()).isEqualTo(87);
    }

    @Test
    void safeAccessors_ShouldTreatMissingCountersAsZero() {
        FavoriteEntity favoriteEntity = FavoriteEntity.builder().userId("user1").build();

        then(favoriteEntity.safeFavorite()).isZero();
        then(favoriteEntity.safeKarmaScore()).isZero();
        then(favoriteEntity.safeAttendanceCount()).isZero();
    }
}
