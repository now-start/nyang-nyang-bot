package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository;

import java.util.List;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteRoundResult;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RouletteRoundResultRepository extends JpaRepository<RouletteRoundResult, Long> {

    List<RouletteRoundResult> findByRouletteEventIdOrderByRoundNoAsc(Long rouletteEventId);

    List<RouletteRoundResult> findByRouletteEventUserIdOrderByCreateDateDesc(String userId);

    List<RouletteRoundResult> findTop5ByRouletteEventUserIdOrderByCreateDateDesc(String userId);

    long countByRouletteEventIdAndStatus(Long rouletteEventId, RouletteRoundStatus status);
}
