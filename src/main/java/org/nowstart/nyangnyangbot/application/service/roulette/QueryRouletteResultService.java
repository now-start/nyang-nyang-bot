package org.nowstart.nyangnyangbot.application.service.roulette;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteEventResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteRoundResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.EventResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;
import org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QueryRouletteResultService implements QueryRouletteResultUseCase {

    private final RoulettePolicy roulettePolicy = new RoulettePolicy();
    private final RoulettePort roulettePort;

    @Override
    @Transactional(readOnly = true)
    public List<RouletteRoundResult> getRecentRounds(String userId, int limit) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        return roulettePort.findRoundsByUserId(userId).stream()
                .limit(roulettePolicy.safeRecentRoundLimit(limit))
                .map(this::roundResult)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouletteEventResult> getUserEvents(String userId) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        return roulettePort.findEventsByUserId(userId).stream()
                .map(event -> eventResult(event, roulettePort.findRoundsByEventId(event.id())))
                .toList();
    }

    private RouletteEventResult eventResult(EventResult event, List<RoundResult> rounds) {
        return new RouletteEventResult(
                event.id(),
                event.donationEventId(),
                event.userId(),
                event.nickNameSnapshot(),
                event.donationAmount(),
                event.roundCount(),
                event.status() == null ? null : event.status().name(),
                event.createdAt(),
                rounds.stream().map(this::roundResult).toList()
        );
    }

    private RouletteRoundResult roundResult(RoundResult round) {
        return new RouletteRoundResult(
                round.id(),
                round.roundNo(),
                round.itemLabel(),
                round.losingItem(),
                round.rewardType() == null ? null : round.rewardType().name(),
                round.conversionMode() == null ? null : round.conversionMode().name(),
                round.exchangeFavoriteValue(),
                round.status() == null ? null : round.status().name(),
                round.ledgerId(),
                round.userUpboId(),
                round.failureReason()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
