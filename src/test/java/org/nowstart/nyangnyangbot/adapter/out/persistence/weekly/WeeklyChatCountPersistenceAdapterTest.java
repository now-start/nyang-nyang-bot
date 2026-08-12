package org.nowstart.nyangnyangbot.adapter.out.persistence.weekly;

import static org.mockito.BDDMockito.then;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.repository.WeeklyChatCountRepository;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatCountPort.IncrementWeeklyChatCommand;

@ExtendWith(MockitoExtension.class)
class WeeklyChatCountPersistenceAdapterTest {

    @Mock
    private WeeklyChatCountRepository repository;

    @InjectMocks
    private WeeklyChatCountPersistenceAdapter adapter;

    @Test
    void increment_ShouldPassWeekStartAsEpochSecondToNativeQuery() {
        Instant weekStartedAt = Instant.parse("2026-08-09T15:00:00Z");

        adapter.increment(new IncrementWeeklyChatCommand(weekStartedAt, "user-1"));

        then(repository).should().increment(weekStartedAt.getEpochSecond(), "user-1");
    }
}
