package org.nowstart.nyangnyangbot.repository;

import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends JpaRepository<FavoriteEntity, String> {

    Page<FavoriteEntity> findByNickNameContains(Pageable pageable, String nickName);
}
