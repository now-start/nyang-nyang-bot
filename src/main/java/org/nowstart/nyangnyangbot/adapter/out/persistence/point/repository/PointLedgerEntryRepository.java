package org.nowstart.nyangnyangbot.adapter.out.persistence.point.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.entity.PointLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointLedgerEntryRepository extends JpaRepository<PointLedgerEntry, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<PointLedgerEntry> findByIdempotencyKey(String idempotencyKey);

    boolean existsByCorrectionOfEntryId(Long correctionOfEntryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entry from PointLedgerEntry entry where entry.id = :id")
    Optional<PointLedgerEntry> findByIdForUpdate(@Param("id") Long id);

    @Query("select coalesce(sum(entry.delta), 0) from PointLedgerEntry entry where entry.userAccount.userId = :userId")
    long balance(@Param("userId") String userId);

    @Query(value = """
            select account.userId as userId,
                   account.displayName as displayName,
                   coalesce(sum(entry.delta), 0) as balance
              from UserAccount account
              left join PointLedgerEntry entry on entry.userAccount = account
             group by account.userId, account.displayName
            """,
            countQuery = "select count(account) from UserAccount account")
    Page<PointSummaryProjection> findPointSummaries(Pageable pageable);

    @Query(value = """
            select account.userId as userId,
                   account.displayName as displayName,
                   coalesce(sum(entry.delta), 0) as balance
              from UserAccount account
              left join PointLedgerEntry entry on entry.userAccount = account
             where lower(coalesce(account.displayName, '')) like lower(concat('%', :displayName, '%'))
             group by account.userId, account.displayName
            """,
            countQuery = """
                    select count(account)
                      from UserAccount account
                     where lower(coalesce(account.displayName, '')) like lower(concat('%', :displayName, '%'))
                    """)
    Page<PointSummaryProjection> findPointSummariesByDisplayName(
            @Param("displayName") String displayName,
            Pageable pageable
    );

    @Query("""
            select account.userId as userId,
                   account.displayName as displayName,
                   coalesce(sum(entry.delta), 0) as balance
              from UserAccount account
              left join PointLedgerEntry entry on entry.userAccount = account
             where account.userId = :userId
             group by account.userId, account.displayName
            """)
    Optional<PointSummaryProjection> findPointSummary(@Param("userId") String userId);

    @Query(value = """
            select count(*)
              from (
                    select account.user_id
                      from user_account account
                      left join point_ledger_entry entry on entry.user_id = account.user_id
                     group by account.user_id
                    having coalesce(sum(entry.delta), 0) > :balance
                   ) ranked_user
            """, nativeQuery = true)
    long countUsersWithBalanceGreaterThan(@Param("balance") long balance);

    @Query(value = """
            select history.id as ledgerId,
                   history.user_id as userId,
                   history.delta as delta,
                   history.balance_after as balanceAfter,
                   history.source_type as sourceType,
                   history.description as description,
                   history.correction_of_entry_id as correctionOfEntryId,
                   history.created_at as createdAt
              from (
                    select entry.*,
                           sum(entry.delta) over (
                               partition by entry.user_id
                               order by entry.created_at, entry.id
                           ) as balance_after
                      from point_ledger_entry entry
                     where entry.user_id = :userId
                   ) history
             order by history.created_at desc, history.id desc
             limit :limit
            """, nativeQuery = true)
    List<PointHistoryProjection> findHistory(@Param("userId") String userId, @Param("limit") int limit);

    interface PointSummaryProjection {
        String getUserId();

        String getDisplayName();

        Long getBalance();
    }

    interface PointHistoryProjection {
        Long getLedgerId();

        String getUserId();

        Long getDelta();

        Long getBalanceAfter();

        String getSourceType();

        String getDescription();

        Long getCorrectionOfEntryId();

        Instant getCreatedAt();
    }
}
