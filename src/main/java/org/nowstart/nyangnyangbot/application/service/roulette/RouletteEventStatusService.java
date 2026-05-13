package org.nowstart.nyangnyangbot.application.service.roulette;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;
import org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy;
import org.nowstart.nyangnyangbot.domain.type.RouletteEventStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RouletteEventStatusService {

    private final RoulettePolicy roulettePolicy = new RoulettePolicy();
    private final RoulettePort roulettePort;

    @Transactional
    public void refreshEventStatus(Long eventId) {
        roulettePort.findEventById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("roulette event not found"));
        List<RoundResult> rounds = roulettePort.findRoundsByEventId(eventId);
        RouletteEventStatus status = roulettePolicy.eventStatus(rounds);
        roulettePort.updateEventStatus(eventId, status);
    }
}
