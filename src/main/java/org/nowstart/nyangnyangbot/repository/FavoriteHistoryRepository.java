package org.nowstart.nyangnyangbot.repository;

import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteHistoryRepository extends JpaRepository<FavoriteHistoryEntity, Long> {

}