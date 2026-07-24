package org.nowstart.nyangnyangbot.application.service.command;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    private static final Instant APPROVED_AT = Instant.parse("2026-07-22T15:00:00Z");
    private static final Instant CALENDAR_DAY_STARTED_AT = Instant.parse("2026-07-22T15:00:00Z");

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
        given(executionPort.lockActiveCommand("!출석")).willReturn(Optional.of(command));
        given(executionPort.currentDatabaseTime()).willReturn(APPROVED_AT);
        given(executionPort.existsCalendarDayStartedAt(7L, "user-1", CALENDAR_DAY_STARTED_AT))
                .willReturn(false);
        given(executionPort.countAll(7L)).willReturn(10L);
        given(executionPort.countForUser(7L, "user-1")).willReturn(3L);
        given(executionPort.findCalendarDayStarts(7L, "user-1"))
                .willReturn(List.of(CALENDAR_DAY_STARTED_AT, CALENDAR_DAY_STARTED_AT.minusSeconds(86_400)));

        var result = service.execute(new ExecuteCommand("!출석", "user-1", "냥이", "", "", ""));

        then(result).isPresent();
        then(result.orElseThrow().renderedMessage()).isEqualTo("10|3|2|2");
        InOrder order = inOrder(executionPort);
        order.verify(executionPort).lockActiveCommand("!출석");
        order.verify(executionPort).observeAndLockUser("user-1", "냥이");
        order.verify(executionPort).currentDatabaseTime();
        order.verify(executionPort).existsCalendarDayStartedAt(7L, "user-1", CALENDAR_DAY_STARTED_AT);
        order.verify(executionPort).append(new ExecutionData(
                7L,
                "user-1",
                APPROVED_AT,
                CommandExecutionPolicy.USER_CALENDAR_DAY,
                null,
                CALENDAR_DAY_STARTED_AT
        ));
        order.verify(executionPort).countAll(7L);
        order.verify(executionPort).countForUser(7L, "user-1");
        order.verify(executionPort).findCalendarDayStarts(7L, "user-1");
    }

    @Test
    void execute_DoesNotQueryCountsOrStreaksWhenTemplateDoesNotUseThem() {
        LockedCommand command = new LockedCommand(
                9L,
                "!인사",
                "안녕 {viewer.nickname}",
                CommandExecutionPolicy.USER_INTERVAL,
                30
        );
        given(executionPort.lockActiveCommand("!인사")).willReturn(Optional.of(command));
        given(executionPort.currentDatabaseTime()).willReturn(APPROVED_AT);
        given(executionPort.findLatestForUpdate(9L, "user-1")).willReturn(Optional.empty());

        var result = service.execute(new ExecuteCommand("!인사", "user-1", "냥이", "", "", ""));

        then(result).isPresent();
        then(result.orElseThrow().renderedMessage()).isEqualTo("안녕 냥이");
        verify(executionPort, never()).countAll(9L);
        verify(executionPort, never()).countForUser(9L, "user-1");
        verify(executionPort, never()).findCalendarDayStarts(9L, "user-1");
    }

    @Test
    void execute_QueriesTotalCountOnlyAfterAppendingWhenTemplateUsesIt() {
        LockedCommand command = new LockedCommand(
                9L,
                "!전체",
                "{count.total}",
                CommandExecutionPolicy.USER_INTERVAL,
                30
        );
        given(executionPort.lockActiveCommand("!전체")).willReturn(Optional.of(command));
        given(executionPort.currentDatabaseTime()).willReturn(APPROVED_AT);
        given(executionPort.findLatestForUpdate(9L, "user-1")).willReturn(Optional.empty());
        given(executionPort.countAll(9L)).willReturn(4L);

        var result = service.execute(new ExecuteCommand("!전체", "user-1", "냥이", "", "", ""));

        then(result).isPresent();
        then(result.orElseThrow().renderedMessage()).isEqualTo("4");
        InOrder order = inOrder(executionPort);
        order.verify(executionPort).append(new ExecutionData(
                9L,
                "user-1",
                APPROVED_AT,
                CommandExecutionPolicy.USER_INTERVAL,
                30,
                null
        ));
        order.verify(executionPort).countAll(9L);
        verify(executionPort, never()).countForUser(9L, "user-1");
        verify(executionPort, never()).findCalendarDayStarts(9L, "user-1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"streak.current", "streak.longest"})
    void execute_QueriesCalendarDayStartsForEachStreakVariableOnly(String streakVariable) {
        LockedCommand command = new LockedCommand(
                7L,
                "!출석",
                "{" + streakVariable + "}",
                CommandExecutionPolicy.USER_CALENDAR_DAY,
                null
        );
        given(executionPort.lockActiveCommand("!출석")).willReturn(Optional.of(command));
        given(executionPort.currentDatabaseTime()).willReturn(APPROVED_AT);
        given(executionPort.existsCalendarDayStartedAt(7L, "user-1", CALENDAR_DAY_STARTED_AT))
                .willReturn(false);
        given(executionPort.findCalendarDayStarts(7L, "user-1"))
                .willReturn(List.of(CALENDAR_DAY_STARTED_AT));

        var result = service.execute(new ExecuteCommand("!출석", "user-1", "냥이", "", "", ""));

        then(result).isPresent();
        then(result.orElseThrow().renderedMessage()).isEqualTo("1");
        InOrder order = inOrder(executionPort);
        order.verify(executionPort).append(new ExecutionData(
                7L,
                "user-1",
                APPROVED_AT,
                CommandExecutionPolicy.USER_CALENDAR_DAY,
                null,
                CALENDAR_DAY_STARTED_AT
        ));
        order.verify(executionPort).findCalendarDayStarts(7L, "user-1");
        verify(executionPort, never()).countAll(7L);
        verify(executionPort, never()).countForUser(7L, "user-1");
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
        given(executionPort.findLatestForUpdate(9L, "user-1"))
                .willReturn(Optional.of(new ExecutionRecord(APPROVED_AT.minusSeconds(29))));

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
        given(executionPort.findLatestForUpdate(9L, "user-1"))
                .willReturn(Optional.of(new ExecutionRecord(APPROVED_AT.minusSeconds(30))));
        given(executionPort.countForUser(9L, "user-1")).willReturn(2L);

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
        verify(executionPort, never()).countAll(9L);
        verify(executionPort, never()).findCalendarDayStarts(9L, "user-1");
    }

    @Test
    void execute_RejectsSecondCalendarDayExecutionWithoutLatestLookup() {
        LockedCommand command = new LockedCommand(
                7L,
                "!출석",
                "ok",
                CommandExecutionPolicy.USER_CALENDAR_DAY,
                null
        );
        given(executionPort.lockActiveCommand("!출석")).willReturn(Optional.of(command));
        given(executionPort.currentDatabaseTime()).willReturn(APPROVED_AT);
        given(executionPort.existsCalendarDayStartedAt(7L, "user-1", CALENDAR_DAY_STARTED_AT))
                .willReturn(true);

        var result = service.execute(new ExecuteCommand("!출석", "user-1", "냥이", "", "", ""));

        then(result).isEmpty();
        verify(executionPort, never()).findLatestForUpdate(7L, "user-1");
        verify(executionPort, never()).append(org.mockito.ArgumentMatchers.any());
    }
}
