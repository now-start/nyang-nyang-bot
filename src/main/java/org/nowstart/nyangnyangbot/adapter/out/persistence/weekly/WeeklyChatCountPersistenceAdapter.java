package org.nowstart.nyangnyangbot.adapter.out.persistence.weekly;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.repository.WeeklyChatCountRepository;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase.WeeklyChatRankView;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatCountPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WeeklyChatCountPersistenceAdapter implements WeeklyChatCountPort {

    private final WeeklyChatCountRepository repository;

    @Override
    public void increment(LocalDate weekStartDate, String userId) {
        repository.increment(weekStartDate, userId);
    }

    @Override
    public List<WeeklyChatRankView> findWeeklyRanks(LocalDate weekStartDate, int limit) {
        var rows = repository.findWeeklyRanks(weekStartDate, PageRequest.of(0, limit));
        List<WeeklyChatRankView> ranks = new ArrayList<>(rows.size());
        int rank = 1;
        for (var row : rows) {
            ranks.add(new WeeklyChatRankView(rank++, row.getDisplayName(), row.getChatCount()));
        }
        return ranks;
    }
}
