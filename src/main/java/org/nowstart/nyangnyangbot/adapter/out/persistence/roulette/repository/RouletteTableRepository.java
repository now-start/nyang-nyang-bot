package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository;

import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteTable;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouletteTableRepository extends JpaRepository<RouletteTable, Long> {

    List<RouletteTable> findAllByOrderByIdDesc();

    List<RouletteTable> findByActiveTrue();

    Optional<RouletteTable> findFirstByActiveTrueOrderByIdDesc();
}
