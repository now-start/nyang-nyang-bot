package org.nowstart.nyangnyangbot.adapter.out.persistence.overlay;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.RoulettePersistenceMapper;
import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.entity.OverlayDisplayEvent;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayDisplayPort.DisplayEventResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;

@Mapper(
        componentModel = "spring",
        uses = RoulettePersistenceMapper.class,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface OverlayDisplayPersistenceMapper {

    @Mapping(target = "rouletteEventId", source = "displayEvent.rouletteEvent.id")
    @Mapping(target = "nickName", source = "displayEvent.rouletteEvent.nickNameSnapshot")
    @Mapping(target = "roundCount", source = "displayEvent.rouletteEvent.roundCount")
    @Mapping(target = "rounds", source = "rounds")
    DisplayEventResult displayEventResult(OverlayDisplayEvent displayEvent, List<RoundResult> rounds);
}
