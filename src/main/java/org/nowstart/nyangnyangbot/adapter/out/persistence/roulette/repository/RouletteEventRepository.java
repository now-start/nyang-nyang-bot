package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository;

import java.util.List;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RouletteEventRepository extends JpaRepository<RouletteEvent, Long> {

    boolean existsByDonationEventId(String donationEventId);

    List<RouletteEvent> findByUserIdOrderByCreateDateDesc(String userId);

    Page<RouletteEvent> findAllByOrderByCreateDateDesc(Pageable pageable);
}
