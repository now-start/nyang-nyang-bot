package org.nowstart.nyangnyangbot.adapter.out.persistence.repository;

import java.util.List;
import org.nowstart.nyangnyangbot.adapter.out.persistence.entity.RouletteEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouletteEventRepository extends JpaRepository<RouletteEventEntity, Long> {

    boolean existsByDonationEventId(String donationEventId);

    List<RouletteEventEntity> findByUserIdOrderByCreateDateDesc(String userId);
}
