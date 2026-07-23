package org.nowstart.nyangnyangbot.adapter.out.persistence.donation.repository;

import java.util.Optional;
import org.nowstart.nyangnyangbot.adapter.out.persistence.donation.entity.Donation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    Optional<Donation> findByIngestionKey(String ingestionKey);
}
