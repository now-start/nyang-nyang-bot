package org.nowstart.nyangnyangbot.adapter.out.persistence.command.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.entity.Command;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommandRepository extends JpaRepository<Command, Long> {

    List<Command> findAllByOrderByIdDesc();

    Optional<Command> findByTriggerToken(String triggerToken);

    List<Command> findByActiveTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select command from Command command where command.id = :id")
    Optional<Command> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select command from Command command where command.triggerToken = :trigger and command.active = true")
    Optional<Command> findActiveByTriggerForUpdate(@Param("trigger") String trigger);
}
