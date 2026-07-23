package org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from UserAccount account where account.userId = :userId")
    Optional<UserAccount> findByIdForUpdate(@Param("userId") String userId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into user_account (
                user_id, display_name, is_admin, last_login_at, created_at, updated_at
            ) values (
                :userId, :displayName, false, null, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
            ) on duplicate key update
                display_name = case
                    when :displayName is null or :displayName = '' then display_name
                    else :displayName
                end,
                updated_at = CURRENT_TIMESTAMP(6)
            """, nativeQuery = true)
    int observe(@Param("userId") String userId, @Param("displayName") String displayName);

    @Query(value = "select CURRENT_TIMESTAMP(6)", nativeQuery = true)
    Instant currentDatabaseTime();
}
