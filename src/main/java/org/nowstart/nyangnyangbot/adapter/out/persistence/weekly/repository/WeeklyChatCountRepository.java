package org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.repository;

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
            values (to_timestamp(:weekStartedAtEpochSecond), :userId, 1)
            on conflict (week_started_at, user_id) do update
            set chat_count = weekly_chat_count.chat_count + 1
            """, nativeQuery = true)
    int increment(
            @Param("weekStartedAtEpochSecond") long weekStartedAtEpochSecond,
            @Param("userId") String userId
    );

    @Query(value = """
            select account.display_name as displayName, weekly.chat_count as chatCount
              from weekly_chat_count weekly
              join user_account account on account.user_id = weekly.user_id
             where weekly.week_started_at = to_timestamp(:weekStartedAtEpochSecond)
             order by weekly.chat_count desc, account.display_name asc, weekly.id asc
            """, nativeQuery = true)
    List<WeeklyChatProjection> findWeeklyRanks(
            @Param("weekStartedAtEpochSecond") long weekStartedAtEpochSecond,
            Pageable pageable
    );

    interface WeeklyChatProjection {
        String getDisplayName();

        Long getChatCount();
    }
}
