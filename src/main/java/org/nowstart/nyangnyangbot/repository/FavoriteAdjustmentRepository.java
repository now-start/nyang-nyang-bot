package org.nowstart.nyangnyangbot.repository;

import java.util.List;
import org.nowstart.nyangnyangbot.data.entity.FavoriteAdjustmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteAdjustmentRepository extends JpaRepository<FavoriteAdjustmentEntity, Long> {

    List<FavoriteAdjustmentEntity> findByOwnerChannelIdOrderByAmountAsc(String ownerChannelId);

    List<FavoriteAdjustmentEntity> findByOwnerChannelIdAndIdIn(String ownerChannelId, List<Long> ids);
}
