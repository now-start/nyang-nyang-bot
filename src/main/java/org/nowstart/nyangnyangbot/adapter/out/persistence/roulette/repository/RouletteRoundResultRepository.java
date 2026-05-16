package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository;

import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteRoundResultEntity;

import java.util.List;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouletteRoundResultRepository extends JpaRepository<RouletteRoundResultEntity, Long> {

    List<RouletteRoundResultEntity> findByRouletteEventIdOrderByRoundNoAsc(Long rouletteEventId);

    List<RouletteRoundResultEntity> findByRouletteEventUserIdOrderByCreateDateDesc(String userId);

    List<RouletteRoundResultEntity> findTop5ByRouletteEventUserIdOrderByCreateDateDesc(String userId);

    long countByRouletteEventIdAndStatus(Long rouletteEventId, RouletteRoundStatus status);
}
