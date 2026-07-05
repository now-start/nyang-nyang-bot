package org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.entity.WeeklyChatRank;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklyChatRankRepository extends JpaRepository<WeeklyChatRank, Long> {

    Optional<WeeklyChatRank> findByWeekStartDateAndUserId(LocalDate weekStartDate, String userId);

    @Query("""
            select w.nickName as nickname, w.chatCount as chatCount
            from WeeklyChatRank w
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
