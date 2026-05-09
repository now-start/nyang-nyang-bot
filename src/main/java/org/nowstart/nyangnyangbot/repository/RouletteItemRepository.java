package org.nowstart.nyangnyangbot.repository;

import java.util.List;
import org.nowstart.nyangnyangbot.data.entity.RouletteItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouletteItemRepository extends JpaRepository<RouletteItemEntity, Long> {

    List<RouletteItemEntity> findByRouletteTableIdOrderByDisplayOrderAscIdAsc(Long rouletteTableId);

    List<RouletteItemEntity> findByRouletteTableIdAndActiveTrueOrderByDisplayOrderAscIdAsc(Long rouletteTableId);
}
