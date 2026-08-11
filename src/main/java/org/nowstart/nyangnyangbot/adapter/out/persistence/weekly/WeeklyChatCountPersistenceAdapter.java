package org.nowstart.nyangnyangbot.adapter.out.persistence.weekly;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.repository.WeeklyChatCountRepository;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatCountPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WeeklyChatCountPersistenceAdapter implements WeeklyChatCountPort {

    private final WeeklyChatCountRepository repository;
    private final OutboundContractValidator contractValidator;

    @Override
    public void increment(IncrementWeeklyChatCommand command) {
        contractValidator.request("weeklyChat.increment", command);
        repository.increment(command.weekStartedAt(), command.userId());
    }

    @Override
    public List<WeeklyChatRankRecord> findWeeklyRanks(Instant weekStartedAt, int limit) {
        var rows = repository.findWeeklyRanks(weekStartedAt, PageRequest.of(0, limit));
        List<WeeklyChatRankRecord> ranks = new ArrayList<>(rows.size());
        int rank = 1;
        for (var row : rows) {
            ranks.add(contractValidator.persistenceResult(
                    "weeklyChat.rank",
                    new WeeklyChatRankRecord(rank++, row.getDisplayName(), row.getChatCount())
            ));
        }
        return ranks;
    }
}
