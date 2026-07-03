package org.nowstart.nyangnyangbot.adapter.out.persistence.command.repository;

import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.entity.Command;
import org.nowstart.nyangnyangbot.domain.type.CommandActionKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommandRepository extends JpaRepository<Command, Long> {

    List<Command> findAllByOrderByIdDesc();

    Optional<Command> findByTriggerToken(String triggerToken);

    Optional<Command> findByActionKey(CommandActionKey actionKey);

    Optional<Command> findByTriggerTokenAndActiveTrue(String triggerToken);

    Optional<Command> findByActionKeyAndActiveTrue(CommandActionKey actionKey);
}
