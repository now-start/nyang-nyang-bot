package org.nowstart.nyangnyangbot.adapter.out.persistence.repository;

import org.nowstart.nyangnyangbot.adapter.out.persistence.entity.DonationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DonationRepository extends JpaRepository<DonationEntity, Long> {

    boolean existsByDonationEventId(String donationEventId);
}
