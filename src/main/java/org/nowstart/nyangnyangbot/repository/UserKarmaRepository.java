package org.nowstart.nyangnyangbot.repository;

import java.util.List;
import org.nowstart.nyangnyangbot.data.entity.UserKarmaEntity;
import org.nowstart.nyangnyangbot.data.type.FavoriteKarmaSourceType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserKarmaRepository extends JpaRepository<UserKarmaEntity, Long> {

    List<UserKarmaEntity> findAllByFavoriteEntityUserId(String userId);

    void deleteAllByFavoriteEntityUserIdAndSourceType(String userId, FavoriteKarmaSourceType sourceType);
}
