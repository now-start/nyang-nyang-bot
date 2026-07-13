package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository;

import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteTable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RouletteTableRepository extends JpaRepository<RouletteTable, Long> {

    List<RouletteTable> findAllByOrderByIdDesc();

    List<RouletteTable> findByActiveTrue();

    Optional<RouletteTable> findFirstByActiveTrueOrderByIdDesc();
}
