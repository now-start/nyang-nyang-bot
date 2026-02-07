package org.nowstart.nyangnyangbot.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.nowstart.nyangnyangbot.data.entity.DonationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DonationRepository extends JpaRepository<DonationEntity, Long> {

    @Query("""
            select d.donatorChannel.name as nickname, sum(d.payAmount) as totalAmount
            from DonationEntity d
            where d.createDate >= :from
              and d.createDate < :to
              and d.donatorChannel is not null
              and d.donatorChannel.name is not null
              and d.donatorChannel.name <> ''
            group by d.donatorChannel.name
            order by sum(d.payAmount) desc
            """)
    List<DonationRankProjection> findWeeklyRanks(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    interface DonationRankProjection {
        String getNickname();

        Long getTotalAmount();
    }
}
