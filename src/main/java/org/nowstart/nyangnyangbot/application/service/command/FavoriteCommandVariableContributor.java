package org.nowstart.nyangnyangbot.application.service.command;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteBalanceQueryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FavoriteCommandVariableContributor implements CommandVariableContributor {

    private static final List<CommandVariableDefinition> DEFINITIONS = List.of(
            new CommandVariableDefinition(
                    "favorite.balance",
                    "호감도",
                    "시청자의 현재 호감도. 데이터가 없으면 0",
                    "100"
            )
    );

    private final FavoriteBalanceQueryPort favoriteBalanceQueryPort;

    @Override
    public List<CommandVariableDefinition> definitions() {
        return DEFINITIONS;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> resolve(Set<String> requestedKeys, CommandVariableContext context) {
        int balance = context.userId() == null || context.userId().isBlank()
                ? 0
                : favoriteBalanceQueryPort.findBalanceByUserId(context.userId()).orElse(0);
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : requestedKeys) {
            values.put(key, String.valueOf(balance));
        }
        return Map.copyOf(values);
    }
}
