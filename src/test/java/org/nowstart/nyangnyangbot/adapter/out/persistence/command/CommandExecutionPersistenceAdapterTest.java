package org.nowstart.nyangnyangbot.adapter.out.persistence.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.repository.CommandExecutionRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.repository.CommandRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.UserAccountRepository;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandExecutionPort.ExecutionData;
import org.nowstart.nyangnyangbot.domain.command.CommandExecutionPolicy;

class CommandExecutionPersistenceAdapterTest {

    @Test
    void appendRejectsIncorrectSeoulCalendarDayStart() {
        CommandExecutionRepository executionRepository = Mockito.mock(CommandExecutionRepository.class);
        CommandExecutionPersistenceAdapter adapter = new CommandExecutionPersistenceAdapter(
                Mockito.mock(CommandRepository.class),
                executionRepository,
                Mockito.mock(UserAccountRepository.class)
        );

        assertThatThrownBy(() -> adapter.append(new ExecutionData(
                1L,
                "viewer",
                Instant.parse("2026-08-13T03:00:00Z"),
                CommandExecutionPolicy.USER_CALENDAR_DAY,
                null,
                Instant.parse("2026-08-11T15:00:00Z")
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("calendarDayStartedAt must be the Asia/Seoul day start");
        Mockito.verify(executionRepository, Mockito.never()).saveAndFlush(Mockito.any());
    }
}
