package org.nowstart.nyangnyangbot.adapter.out.persistence.authorization;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.nowstart.nyangnyangbot.adapter.out.persistence.authorization.entity.AuthorizationAccount;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort.AuthorizationAccountResult;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AuthorizationPersistenceMapper {

    AuthorizationAccountResult accountResult(AuthorizationAccount entity);
}
