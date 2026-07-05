package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository;

import java.util.List;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouletteItemRepository extends JpaRepository<RouletteItem, Long> {

    List<RouletteItem> findByRouletteTableIdOrderByDisplayOrderAscIdAsc(Long rouletteTableId);

    List<RouletteItem> findByRouletteTableIdAndActiveTrueOrderByDisplayOrderAscIdAsc(Long rouletteTableId);
}
