package org.nowstart.nyangnyangbot.adapter.out.persistence.weekly;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.entity.WeeklyChatRank;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatRankPort.WeeklyChatRankRecordResult;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface WeeklyChatRankPersistenceMapper {

    WeeklyChatRankRecordResult recordResult(WeeklyChatRank entity);
}
