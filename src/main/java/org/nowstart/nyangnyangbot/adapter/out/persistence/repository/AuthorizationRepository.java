package org.nowstart.nyangnyangbot.adapter.out.persistence.repository;

import org.nowstart.nyangnyangbot.adapter.out.persistence.entity.AuthorizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorizationRepository extends JpaRepository<AuthorizationEntity, String> {

}
