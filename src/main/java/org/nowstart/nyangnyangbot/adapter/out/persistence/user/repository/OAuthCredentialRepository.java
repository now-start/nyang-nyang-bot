package org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.OAuthCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OAuthCredentialRepository extends JpaRepository<OAuthCredential, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select credential from OAuthCredential credential where credential.userId = :userId")
    Optional<OAuthCredential> findByIdForUpdate(@Param("userId") String userId);
}
