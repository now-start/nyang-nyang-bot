package org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository;

import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteAdjustmentEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteAdjustmentRepository extends JpaRepository<FavoriteAdjustmentEntity, Long> {
}
