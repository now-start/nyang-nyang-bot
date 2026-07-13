package org.nowstart.nyangnyangbot.adapter.out.persistence.authorization.repository;

import org.nowstart.nyangnyangbot.adapter.out.persistence.authorization.entity.AuthorizationAccount;

import org.springframework.data.jpa.repository.JpaRepository;
public interface AuthorizationRepository extends JpaRepository<AuthorizationAccount, String> {

}
