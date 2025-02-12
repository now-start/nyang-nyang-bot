package org.nowstart.nyangnyangbot.repository;

import org.nowstart.nyangnyangbot.data.entity.AuthorizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorizationRepository extends JpaRepository<AuthorizationEntity, String> {
}
