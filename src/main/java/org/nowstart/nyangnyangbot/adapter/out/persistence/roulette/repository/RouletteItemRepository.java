package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository;

import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteItemEntity;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouletteItemRepository extends JpaRepository<RouletteItemEntity, Long> {

    List<RouletteItemEntity> findByRouletteTableIdOrderByDisplayOrderAscIdAsc(Long rouletteTableId);

    List<RouletteItemEntity> findByRouletteTableIdAndActiveTrueOrderByDisplayOrderAscIdAsc(Long rouletteTableId);
}
