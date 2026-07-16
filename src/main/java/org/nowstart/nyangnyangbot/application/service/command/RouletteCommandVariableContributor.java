package org.nowstart.nyangnyangbot.application.service.command;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RecentRouletteResultQueryPort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RecentRouletteResultQueryPort.RecentRound;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RouletteCommandVariableContributor implements CommandVariableContributor {

    private static final String VARIABLE_KEY = "roulette.recentSummary";
    private static final List<CommandVariableDefinition> DEFINITIONS = List.of(
            new CommandVariableDefinition(
                    VARIABLE_KEY,
                    "최근 룰렛 결과",
                    "시청자의 최근 룰렛 결과 요약",
                    "최근 룰렛 결과: 1회차 당첨, 2회차 꽝"
            )
    );

    private final RecentRouletteResultQueryPort recentRouletteResultQueryPort;

    @Override
    public List<CommandVariableDefinition> definitions() {
        return DEFINITIONS;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> resolve(Set<String> requestedKeys, CommandVariableContext context) {
        if (!requestedKeys.contains(VARIABLE_KEY)) {
            return Map.of();
        }
        List<RecentRound> rounds = context.userId() == null || context.userId().isBlank()
                ? List.of()
                : recentRouletteResultQueryPort.findRecentRoundsByUserId(context.userId());
        if (rounds.isEmpty()) {
            return Map.of(VARIABLE_KEY, "최근 룰렛 결과가 없습니다.");
        }
        String summary = rounds.stream()
                .map(round -> round.roundNo() + "회차 " + round.itemLabel())
                .collect(Collectors.joining(", "));
        return Map.of(VARIABLE_KEY, "최근 룰렛 결과: " + summary);
    }
}
