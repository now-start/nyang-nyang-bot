package org.nowstart.nyangnyangbot.adapter.out.persistence.command.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.entity.CommandExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommandExecutionRepository extends JpaRepository<CommandExecution, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CommandExecution> findFirstByCommandIdAndUserAccountUserIdOrderByExecutedAtDescIdDesc(
            Long commandId,
            String userId
    );

    long countByCommandId(Long commandId);

    long countByCommandIdAndUserAccountUserId(Long commandId, String userId);

    boolean existsByCommandIdAndUserAccountUserIdAndCalendarDate(
            Long commandId,
            String userId,
            LocalDate calendarDate
    );

    @Query("""
            select distinct execution.calendarDate
              from CommandExecution execution
             where execution.command.id = :commandId
               and execution.userAccount.userId = :userId
               and execution.calendarDate is not null
             order by execution.calendarDate desc
            """)
    List<LocalDate> findExecutionDates(
            @Param("commandId") Long commandId,
            @Param("userId") String userId
    );
}
