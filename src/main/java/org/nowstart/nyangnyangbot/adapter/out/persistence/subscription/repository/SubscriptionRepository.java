package org.nowstart.nyangnyangbot.adapter.out.persistence.subscription.repository;

import org.nowstart.nyangnyangbot.adapter.out.persistence.subscription.entity.SubscriptionEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {
}
