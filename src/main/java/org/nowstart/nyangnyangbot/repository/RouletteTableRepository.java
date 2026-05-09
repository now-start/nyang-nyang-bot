package org.nowstart.nyangnyangbot.repository;

import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.data.entity.RouletteTableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouletteTableRepository extends JpaRepository<RouletteTableEntity, Long> {

    List<RouletteTableEntity> findAllByOrderByIdDesc();

    List<RouletteTableEntity> findByActiveTrue();

    Optional<RouletteTableEntity> findFirstByActiveTrueOrderByIdDesc();
}
