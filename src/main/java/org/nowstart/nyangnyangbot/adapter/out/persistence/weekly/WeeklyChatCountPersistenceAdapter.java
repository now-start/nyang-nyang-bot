package org.nowstart.nyangnyangbot.adapter.out.persistence.weekly;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.repository.WeeklyChatCountRepository;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatCountPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@RequiredArgsConstructor
public class WeeklyChatCountPersistenceAdapter implements WeeklyChatCountPort {

    private final WeeklyChatCountRepository repository;

    @Override
    public void increment(IncrementWeeklyChatCommand command) {
        repository.increment(command.weekStartedAt().getEpochSecond(), command.userId());
    }

    @Override
    public List<WeeklyChatRankRecord> findWeeklyRanks(Instant weekStartedAt, int limit) {
        var rows = repository.findWeeklyRanks(weekStartedAt, PageRequest.of(0, limit));
        List<WeeklyChatRankRecord> ranks = new ArrayList<>(rows.size());
        int rank = 1;
        for (var row : rows) {
            ranks.add(new WeeklyChatRankRecord(rank++, row.getDisplayName(), row.getChatCount()));
        }
        return ranks;
    }
}
