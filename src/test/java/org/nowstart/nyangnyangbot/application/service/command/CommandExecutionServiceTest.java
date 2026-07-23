package org.nowstart.nyangnyangbot.application.service.command;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.command.ExecuteCommandUseCase.ExecuteCommand;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandExecutionPort;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandExecutionPort.ExecutionData;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandExecutionPort.ExecutionRecord;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandExecutionPort.LockedCommand;
import org.nowstart.nyangnyangbot.domain.command.CommandExecutionPolicy;

@ExtendWith(MockitoExtension.class)
class CommandExecutionServiceTest {

    private static final Instant APPROVED_AT = Instant.parse("2026-07-23T00:00:00Z");

    @Mock
    private CommandExecutionPort executionPort;

    private CommandExecutionService service;

    @BeforeEach
    void setUp() {
        CommandTemplateRenderer renderer = new CommandTemplateRenderer();
        CommandVariableRegistry registry = new CommandVariableRegistry(List.of(new CoreCommandVariableContributor()));
        service = new CommandExecutionService(executionPort, renderer, registry);
    }

    @Test
    void execute_AppendsDailyEventAndRendersCountsAndStreakInsideLockOrder() {
        LockedCommand command = new LockedCommand(
                7L,
                "!출석",
                "{count.total}|{count.user}|{streak.current}|{streak.longest}",
                CommandExecutionPolicy.USER_CALENDAR_DAY,
                null
        );
        LocalDate today = LocalDate.of(2026, 7, 23);
        given(executionPort.lockActiveCommand("!출석")).willReturn(Optional.of(command));
        given(executionPort.currentDatabaseTime()).willReturn(APPROVED_AT);
        given(executionPort.findLatestForUpdate(7L, "user-1")).willReturn(Optional.empty());
        given(executionPort.existsCalendarDate(7L, "user-1", today)).willReturn(false);
        given(executionPort.countAll(7L)).willReturn(10L);
        given(executionPort.countForUser(7L, "user-1")).willReturn(3L);
        given(executionPort.findExecutionDates(7L, "user-1"))
                .willReturn(List.of(today, today.minusDays(1)));

        var result = service.execute(new ExecuteCommand("!출석", "user-1", "냥이", "", "", ""));

        then(result).isPresent();
        then(result.orElseThrow().renderedMessage()).isEqualTo("10|3|2|2");
        InOrder order = inOrder(executionPort);
        order.verify(executionPort).lockActiveCommand("!출석");
        order.verify(executionPort).observeAndLockUser("user-1", "냥이");
        order.verify(executionPort).currentDatabaseTime();
        order.verify(executionPort).findLatestForUpdate(7L, "user-1");
        order.verify(executionPort).existsCalendarDate(7L, "user-1", today);
        order.verify(executionPort).append(new ExecutionData(
                7L,
                "user-1",
                APPROVED_AT,
                CommandExecutionPolicy.USER_CALENDAR_DAY,
                null,
                today
        ));
    }

    @Test
    void execute_RejectsIntervalBeforeCooldownWithoutAppending() {
        LockedCommand command = new LockedCommand(
                9L,
                "!카운트",
                "ok",
                CommandExecutionPolicy.USER_INTERVAL,
                30
        );
        given(executionPort.lockActiveCommand("!카운트")).willReturn(Optional.of(command));
        given(executionPort.currentDatabaseTime()).willReturn(APPROVED_AT);
        given(executionPort.findLatestForUpdate(9L, "user-1")).willReturn(Optional.of(new ExecutionRecord(
                1L,
                APPROVED_AT.minusSeconds(29),
                CommandExecutionPolicy.USER_INTERVAL,
                30,
                null
        )));

        var result = service.execute(new ExecuteCommand("!카운트", "user-1", "냥이", "", "", ""));

        then(result).isEmpty();
        verify(executionPort, never()).append(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void execute_AllowsIntervalAtExactCooldownBoundary() {
        LockedCommand command = new LockedCommand(
                9L,
                "!카운트",
                "{count.user}",
                CommandExecutionPolicy.USER_INTERVAL,
                30
        );
        given(executionPort.lockActiveCommand("!카운트")).willReturn(Optional.of(command));
        given(executionPort.currentDatabaseTime()).willReturn(APPROVED_AT);
        given(executionPort.findLatestForUpdate(9L, "user-1")).willReturn(Optional.of(new ExecutionRecord(
                1L,
                APPROVED_AT.minusSeconds(30),
                CommandExecutionPolicy.USER_INTERVAL,
                30,
                null
        )));
        given(executionPort.countAll(9L)).willReturn(2L);
        given(executionPort.countForUser(9L, "user-1")).willReturn(2L);
        given(executionPort.findExecutionDates(9L, "user-1")).willReturn(List.of());

        var result = service.execute(new ExecuteCommand("!카운트", "user-1", "냥이", "", "", ""));

        then(result).isPresent();
        then(result.orElseThrow().renderedMessage()).isEqualTo("2");
        verify(executionPort).append(new ExecutionData(
                9L,
                "user-1",
                APPROVED_AT,
                CommandExecutionPolicy.USER_INTERVAL,
                30,
                null
        ));
    }

    @Test
    void execute_RejectsSecondCalendarDayExecutionEvenWhenLatestLookupChanges() {
        LocalDate today = LocalDate.of(2026, 7, 23);
        LockedCommand command = new LockedCommand(
                7L,
                "!출석",
                "ok",
                CommandExecutionPolicy.USER_CALENDAR_DAY,
                null
        );
        given(executionPort.lockActiveCommand("!출석")).willReturn(Optional.of(command));
        given(executionPort.currentDatabaseTime()).willReturn(APPROVED_AT);
        given(executionPort.findLatestForUpdate(7L, "user-1")).willReturn(Optional.empty());
        given(executionPort.existsCalendarDate(7L, "user-1", today)).willReturn(true);

        var result = service.execute(new ExecuteCommand("!출석", "user-1", "냥이", "", "", ""));

        then(result).isEmpty();
        verify(executionPort, never()).append(org.mockito.ArgumentMatchers.any());
    }
}
