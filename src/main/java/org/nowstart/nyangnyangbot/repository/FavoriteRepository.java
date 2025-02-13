package org.nowstart.nyangnyangbot.repository;

import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends JpaRepository<FavoriteEntity, String> {

}
