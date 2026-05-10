package org.nowstart.nyangnyangbot.adapter.out.persistence.repository;

import org.nowstart.nyangnyangbot.adapter.out.persistence.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {
}
