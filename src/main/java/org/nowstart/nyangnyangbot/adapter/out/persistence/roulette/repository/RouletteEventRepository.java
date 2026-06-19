package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository;

import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteEvent;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouletteEventRepository extends JpaRepository<RouletteEvent, Long> {

    boolean existsByDonationEventId(String donationEventId);

    List<RouletteEvent> findByUserIdOrderByCreateDateDesc(String userId);
}
