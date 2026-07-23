package org.nowstart.nyangnyangbot.application.service.command;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.out.point.PointQueryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PointCommandVariableContributor implements CommandVariableContributor {

    private static final List<CommandVariableDefinition> DEFINITIONS = List.of(
            new CommandVariableDefinition(
                    "point.balance",
                    "포인트",
                    "시청자의 현재 포인트. 데이터가 없으면 0",
                    "100"
            )
    );

    private final PointQueryPort pointQueryPort;

    @Override
    public List<CommandVariableDefinition> definitions() {
        return DEFINITIONS;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> resolve(Set<String> requestedKeys, CommandVariableContext context) {
        long balance = context.userId() == null || context.userId().isBlank()
                ? 0
                : pointQueryPort.findBalanceByUserId(context.userId()).orElse(0L);
        Map<String, String> values = new LinkedHashMap<>();
        requestedKeys.forEach(key -> values.put(key, String.valueOf(balance)));
        return Map.copyOf(values);
    }
}
