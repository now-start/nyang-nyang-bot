package org.nowstart.nyangnyangbot.repository;

import org.nowstart.nyangnyangbot.data.entity.DonationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DonationRepository extends JpaRepository<DonationEntity, Long> {
}
