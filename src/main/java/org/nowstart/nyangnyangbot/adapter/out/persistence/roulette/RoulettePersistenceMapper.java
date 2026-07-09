package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteEvent;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteItem;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteRoundResult;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteTable;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.EventResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.ItemResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.TableResult;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RoulettePersistenceMapper {

    TableResult tableResult(RouletteTable entity);

    @Mapping(target = "tableId", source = "rouletteTable.id")
    ItemResult itemResult(RouletteItem entity);

    @Mapping(target = "createdAt", source = "createDate")
    EventResult eventResult(RouletteEvent entity);

    @Mapping(target = "rouletteEventId", source = "rouletteEvent.id")
    @Mapping(target = "rouletteEventDonationEventId", source = "rouletteEvent.donationEventId")
    @Mapping(target = "rouletteEventUserId", source = "rouletteEvent.userId")
    @Mapping(target = "rouletteEventNickNameSnapshot", source = "rouletteEvent.nickNameSnapshot")
    RoundResult roundResult(RouletteRoundResult entity);
}
