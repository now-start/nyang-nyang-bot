package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository;

import java.util.List;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouletteOptionRepository extends JpaRepository<RouletteOption, Long> {

    List<RouletteOption> findByRouletteConfig_IdOrderByDisplayOrderAscIdAsc(Long rouletteConfigId);
}
