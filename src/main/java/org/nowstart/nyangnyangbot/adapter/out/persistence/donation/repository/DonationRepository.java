package org.nowstart.nyangnyangbot.adapter.out.persistence.donation.repository;

import org.nowstart.nyangnyangbot.adapter.out.persistence.donation.entity.Donation;

import org.springframework.data.jpa.repository.JpaRepository;
public interface DonationRepository extends JpaRepository<Donation, Long> {

    boolean existsByDonationEventId(String donationEventId);
}
