package org.nowstart.nyangnyangbot.adapter.out.persistence.subscription.repository;

import org.nowstart.nyangnyangbot.adapter.out.persistence.subscription.entity.Subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
}
