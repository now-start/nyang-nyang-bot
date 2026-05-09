package org.nowstart.nyangnyangbot.repository;

import java.util.List;
import org.nowstart.nyangnyangbot.data.entity.RouletteRoundResultEntity;
import org.nowstart.nyangnyangbot.data.type.RouletteRoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouletteRoundResultRepository extends JpaRepository<RouletteRoundResultEntity, Long> {

    List<RouletteRoundResultEntity> findByRouletteEventIdOrderByRoundNoAsc(Long rouletteEventId);

    List<RouletteRoundResultEntity> findByRouletteEventUserIdOrderByCreateDateDesc(String userId);

    List<RouletteRoundResultEntity> findTop5ByRouletteEventUserIdOrderByCreateDateDesc(String userId);

    long countByRouletteEventIdAndStatus(Long rouletteEventId, RouletteRoundStatus status);
}
