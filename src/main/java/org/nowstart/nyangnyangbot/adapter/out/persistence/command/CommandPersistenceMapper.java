package org.nowstart.nyangnyangbot.adapter.out.persistence.command;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.entity.Command;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.CommandRecord;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CommandPersistenceMapper {

    @Mapping(target = "trigger", source = "triggerToken")
    CommandRecord commandRecord(Command entity);
}
