package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteConfig;
import org.nowstart.nyangnyangbot.domain.type.RouletteConfigStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RouletteConfigRepository extends JpaRepository<RouletteConfig, Long> {

    Page<RouletteConfig> findAllByOrderByIdDesc(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select config from RouletteConfig config where config.id = :id")
    Optional<RouletteConfig> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select config from RouletteConfig config where config.status = :status order by config.id")
    List<RouletteConfig> findByStatusForUpdate(@Param("status") RouletteConfigStatus status);
}
