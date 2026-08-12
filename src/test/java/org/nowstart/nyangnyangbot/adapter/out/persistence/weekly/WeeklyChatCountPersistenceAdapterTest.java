package org.nowstart.nyangnyangbot.adapter.out.persistence.weekly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.repository.WeeklyChatCountRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.UserAccountRepository;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatCountPort.IncrementWeeklyChatCommand;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class WeeklyChatCountPersistenceAdapterTest {

    @Mock
    private WeeklyChatCountRepository repository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private WeeklyChatCountPersistenceAdapter adapter;

    @Test
    void currentDatabaseTime_ShouldUseSharedDatabaseClock() {
        Instant databaseTime = Instant.parse("2026-08-12T11:00:00Z");
        given(userAccountRepository.currentDatabaseTime()).willReturn(databaseTime);

        assertThat(adapter.currentDatabaseTime()).isEqualTo(databaseTime);
    }

    @Test
    void increment_ShouldPassWeekStartAsEpochSecondToNativeQuery() {
        Instant weekStartedAt = Instant.parse("2026-08-09T15:00:00Z");

        adapter.increment(new IncrementWeeklyChatCommand(weekStartedAt, "user-1"));

        then(repository).should().increment(weekStartedAt.getEpochSecond(), "user-1");
    }

    @Test
    void findWeeklyRanks_ShouldPassWeekStartAsEpochSecondToNativeQuery() {
        Instant weekStartedAt = Instant.parse("2026-08-09T15:00:00Z");
        given(repository.findWeeklyRanks(weekStartedAt.getEpochSecond(), PageRequest.of(0, 10)))
                .willReturn(List.of());

        adapter.findWeeklyRanks(weekStartedAt, 10);

        then(repository).should().findWeeklyRanks(
                weekStartedAt.getEpochSecond(),
                PageRequest.of(0, 10)
        );
    }
}
