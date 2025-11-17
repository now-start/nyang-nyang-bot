package org.nowstart.nyangnyangbot.data.entity;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class FavoriteEntityTest {

    @Test
    void favoriteEntity_ShouldBuildCorrectly() {
        // when
        FavoriteEntity entity = FavoriteEntity.builder()
                .userId("user123")
                .nickName("테스트유저")
                .favorite(100)
                .build();

        // then
        then(entity.getUserId()).isEqualTo("user123");
        then(entity.getNickName()).isEqualTo("테스트유저");
        then(entity.getFavorite()).isEqualTo(100);
    }

    @Test
    void favoriteEntity_ShouldInitializeHistoryList() {
        // when
        FavoriteEntity entity = FavoriteEntity.builder()
                .userId("user123")
                .nickName("유저")
                .favorite(0)
                .build();

        // then
        then(entity.getFavoriteHistoryEntityList()).isNotNull();
        then(entity.getFavoriteHistoryEntityList()).isEmpty();
    }

    @Test
    void favoriteEntity_ShouldSupportSetters() {
        // given
        FavoriteEntity entity = new FavoriteEntity();

        // when
        entity.setUserId("newUser");
        entity.setNickName("새이름");
        entity.setFavorite(50);

        // then
        then(entity.getUserId()).isEqualTo("newUser");
        then(entity.getNickName()).isEqualTo("새이름");
        then(entity.getFavorite()).isEqualTo(50);
    }

    @Test
    void favoriteEntity_ShouldAddHistory() {
        // given
        FavoriteEntity entity = FavoriteEntity.builder()
                .userId("user123")
                .nickName("유저")
                .favorite(100)
                .favoriteHistoryEntityList(new ArrayList<>())
                .build();

        FavoriteHistoryEntity history = FavoriteHistoryEntity.builder()
                .favoriteEntity(entity)
                .history("테스트 히스토리")
                .favorite(100)
                .build();

        // when
        entity.getFavoriteHistoryEntityList().add(history);

        // then
        then(entity.getFavoriteHistoryEntityList()).hasSize(1);
        then(entity.getFavoriteHistoryEntityList().get(0).getHistory()).isEqualTo("테스트 히스토리");
    }

    @Test
    void favoriteEntity_ShouldHandleNegativeFavorite() {
        // when
        FavoriteEntity entity = FavoriteEntity.builder()
                .userId("user123")
                .nickName("제재유저")
                .favorite(-100)
                .build();

        // then
        then(entity.getFavorite()).isEqualTo(-100);
    }
}
