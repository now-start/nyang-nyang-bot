package org.nowstart.nyangnyangbot.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.data.entity.WeeklyChatRankEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklyChatRankRepository extends JpaRepository<WeeklyChatRankEntity, Long> {

    Optional<WeeklyChatRankEntity> findByWeekStartDateAndUserId(LocalDate weekStartDate, String userId);

    @Query("""
            select w.nickName as nickname, w.chatCount as chatCount
            from WeeklyChatRankEntity w
            where w.weekStartDate = :weekStartDate
            order by w.chatCount desc, w.nickName asc
            """)
    List<WeeklyChatRankProjection> findWeeklyRanks(
            @Param("weekStartDate") LocalDate weekStartDate,
            Pageable pageable
    );

    interface WeeklyChatRankProjection {
        String getNickname();

        Long getChatCount();
    }
}
