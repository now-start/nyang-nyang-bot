package org.nowstart.nyangnyangbot.adapter.out.persistence.favorite;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteAdjustment;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteAdjustmentPort.OptionResult;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FavoriteAdjustmentPersistenceMapper {

    OptionResult optionResult(FavoriteAdjustment entity);
}
