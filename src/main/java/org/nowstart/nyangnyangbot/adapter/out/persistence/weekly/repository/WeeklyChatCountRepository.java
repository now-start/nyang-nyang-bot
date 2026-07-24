package org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.repository;

import java.time.Instant;
import java.util.List;
import org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.entity.WeeklyChatCount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeeklyChatCountRepository extends JpaRepository<WeeklyChatCount, Long> {

    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into weekly_chat_count (week_started_at, user_id, chat_count)
            values (:weekStartedAt, :userId, 1)
            on duplicate key update chat_count = chat_count + 1
            """, nativeQuery = true)
    int increment(@Param("weekStartedAt") Instant weekStartedAt, @Param("userId") String userId);

    @Query("""
            select weekly.userAccount.displayName as displayName, weekly.chatCount as chatCount
              from WeeklyChatCount weekly
             where weekly.weekStartedAt = :weekStartedAt
             order by weekly.chatCount desc, weekly.userAccount.displayName asc, weekly.id asc
            """)
    List<WeeklyChatProjection> findWeeklyRanks(
            @Param("weekStartedAt") Instant weekStartedAt,
            Pageable pageable
    );

    interface WeeklyChatProjection {
        String getDisplayName();

        Long getChatCount();
    }
}
