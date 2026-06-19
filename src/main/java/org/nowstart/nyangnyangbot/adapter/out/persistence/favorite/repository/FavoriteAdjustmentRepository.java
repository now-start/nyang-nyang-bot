package org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository;

import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteAdjustment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteAdjustmentRepository extends JpaRepository<FavoriteAdjustment, Long> {
}
