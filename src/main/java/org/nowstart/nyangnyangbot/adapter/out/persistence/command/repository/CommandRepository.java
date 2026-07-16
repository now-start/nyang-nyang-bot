package org.nowstart.nyangnyangbot.adapter.out.persistence.command.repository;

import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.entity.Command;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommandRepository extends JpaRepository<Command, Long> {

    List<Command> findAllByOrderByIdDesc();

    Optional<Command> findByTriggerToken(String triggerToken);

    Optional<Command> findByTriggerTokenAndActiveTrue(String triggerToken);
}
