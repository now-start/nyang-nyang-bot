package org.nowstart.nyangnyangbot.adapter.out.persistence.timer.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.nowstart.nyangnyangbot.adapter.out.persistence.timer.entity.TimerMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimerMessageRepository extends JpaRepository<TimerMessage, Long> {

    List<TimerMessage> findAllByOrderByIdDesc();

    Optional<TimerMessage> findByIdAndClaimToken(Long id, String claimToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select timer from TimerMessage timer where timer.id = :id")
    Optional<TimerMessage> findByIdForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TimerMessage timer
               set timer.chatCountSinceLastSend = timer.chatCountSinceLastSend + 1
             where timer.active = true
            """)
    int incrementActiveChatCounts();

    @Query("""
            select timer.id
              from TimerMessage timer
             where timer.active = true
               and timer.nextRunAt <= :now
               and timer.chatCountSinceLastSend >= timer.minChatCount
               and (timer.claimExpiresAt is null or timer.claimExpiresAt < :now)
             order by timer.nextRunAt, timer.id
            """)
    List<Long> findClaimCandidateIds(@Param("now") LocalDateTime now, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TimerMessage timer
               set timer.claimToken = :claimToken,
                   timer.claimExpiresAt = :claimExpiresAt,
                   timer.claimedChatCount = timer.chatCountSinceLastSend
             where timer.id = :timerMessageId
               and timer.active = true
               and timer.nextRunAt <= :now
               and timer.chatCountSinceLastSend >= timer.minChatCount
               and (timer.claimExpiresAt is null or timer.claimExpiresAt < :now)
            """)
    int claimDue(
            @Param("timerMessageId") Long timerMessageId,
            @Param("claimToken") String claimToken,
            @Param("now") LocalDateTime now,
            @Param("claimExpiresAt") LocalDateTime claimExpiresAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TimerMessage timer
               set timer.chatCountSinceLastSend =
                       case when timer.active = true
                                  and timer.intervalMinutes = :claimedIntervalMinutes
                                  and timer.nextRunAt = :claimedNextRunAt
                            then case when timer.chatCountSinceLastSend >= timer.claimedChatCount
                                      then timer.chatCountSinceLastSend - timer.claimedChatCount
                                      else 0 end
                            else timer.chatCountSinceLastSend end,
                   timer.claimedChatCount = 0,
                   timer.claimToken = null,
                   timer.claimExpiresAt = null,
                   timer.lastSentAt = :sentAt,
                   timer.nextRunAt =
                       case when timer.active = true
                                  and timer.intervalMinutes = :claimedIntervalMinutes
                                  and timer.nextRunAt = :claimedNextRunAt
                            then :nextRunAt
                            else timer.nextRunAt end
             where timer.id = :timerMessageId
               and timer.claimToken = :claimToken
            """)
    int completeClaim(
            @Param("timerMessageId") Long timerMessageId,
            @Param("claimToken") String claimToken,
            @Param("claimedNextRunAt") LocalDateTime claimedNextRunAt,
            @Param("claimedIntervalMinutes") Integer claimedIntervalMinutes,
            @Param("sentAt") LocalDateTime sentAt,
            @Param("nextRunAt") LocalDateTime nextRunAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TimerMessage timer
               set timer.claimedChatCount = 0,
                   timer.claimToken = null,
                   timer.claimExpiresAt = null,
                   timer.nextRunAt =
                       case when timer.active = true
                                  and timer.intervalMinutes = :claimedIntervalMinutes
                                  and timer.nextRunAt = :claimedNextRunAt
                            then :retryAt
                            else timer.nextRunAt end
             where timer.id = :timerMessageId
               and timer.claimToken = :claimToken
            """)
    int releaseClaim(
            @Param("timerMessageId") Long timerMessageId,
            @Param("claimToken") String claimToken,
            @Param("claimedNextRunAt") LocalDateTime claimedNextRunAt,
            @Param("claimedIntervalMinutes") Integer claimedIntervalMinutes,
            @Param("retryAt") LocalDateTime retryAt
    );
}
