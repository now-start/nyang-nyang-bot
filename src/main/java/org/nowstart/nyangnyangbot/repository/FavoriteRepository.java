package org.nowstart.nyangnyangbot.repository;

import java.util.Optional;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends JpaRepository<FavoriteEntity, Long> {

    Page<FavoriteEntity> findByOwnerChannelId(Pageable pageable, String ownerChannelId);

    Page<FavoriteEntity> findByOwnerChannelIdAndTargetChannelNameContains(Pageable pageable, String ownerChannelId, String nickName);

    Optional<FavoriteEntity> findByOwnerChannelIdAndTargetChannelId(String ownerChannelId, String targetChannelId);
}
