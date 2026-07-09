package org.nowstart.nyangnyangbot.adapter.out.persistence.upbo;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.entity.UpboTemplate;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.entity.UserUpbo;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.TemplateResult;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.UserResult;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UpboPersistenceMapper {

    TemplateResult templateResult(UpboTemplate entity);

    @Mapping(target = "upboTemplateId", source = "upboTemplate.id")
    @Mapping(target = "createdAt", source = "createDate")
    UserResult userResult(UserUpbo entity);
}
