package org.nowstart.nyangnyangbot.application.service.command;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.command.ExecuteCommandUseCase;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandExecutionPort;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandExecutionPort.ExecutionData;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandExecutionPort.ExecutionRecord;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandExecutionPort.LockedCommand;
import org.nowstart.nyangnyangbot.domain.chat.CommandTrigger;
import org.nowstart.nyangnyangbot.domain.command.CommandExecutionPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommandExecutionService implements ExecuteCommandUseCase {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final CommandExecutionPort executionPort;
    private final CommandTemplateRenderer templateRenderer;
    private final CommandVariableRegistry variableRegistry;

    @Override
    @Transactional
    public Optional<ApprovedCommand> execute(ExecuteCommand request) {
        if (request == null || request.userId() == null || request.userId().isBlank()) {
            return Optional.empty();
        }
        String normalizedTrigger = CommandTrigger.normalize(request.trigger());
        Optional<LockedCommand> locked = executionPort.lockActiveCommand(normalizedTrigger);
        if (locked.isEmpty()) {
            return Optional.empty();
        }
        LockedCommand command = locked.get();

        // 모든 writer의 잠금 순서는 command -> user_account 이다.
        executionPort.observeAndLockUser(request.userId(), request.displayName());
        Instant approvedAt = executionPort.currentDatabaseTime();
        LocalDate today = approvedAt.atZone(SEOUL).toLocalDate();
        LocalDate calendarDate = command.executionPolicy() == CommandExecutionPolicy.USER_CALENDAR_DAY
                ? today
                : null;

        if (!isAllowed(command, request.userId(), approvedAt, calendarDate)) {
            return Optional.empty();
        }

        executionPort.append(new ExecutionData(
                command.id(),
                request.userId(),
                approvedAt,
                command.executionPolicy(),
                command.userCooldownSeconds(),
                calendarDate
        ));

        long totalCount = executionPort.countAll(command.id());
        long userCount = executionPort.countForUser(command.id(), request.userId());
        Streaks streaks = streaks(executionPort.findExecutionDates(command.id(), request.userId()), today);
        LocalDateTime localNow = LocalDateTime.ofInstant(approvedAt, SEOUL);
        CommandVariableContext context = new CommandVariableContext(
                request.userId(),
                request.displayName(),
                command.trigger(),
                request.args(),
                request.arg1(),
                request.arg2(),
                localNow,
                totalCount,
                userCount,
                streaks.current(),
                streaks.longest()
        );
        Set<String> variables = templateRenderer.variables(command.messageTemplate());
        String rendered = templateRenderer.render(
                command.messageTemplate(),
                variableRegistry.resolve(variables, context)
        );
        if (rendered.isBlank()) {
            throw new IllegalStateException("Command response rendered blank");
        }
        return Optional.of(new ApprovedCommand(
                command.id(),
                command.trigger(),
                rendered
        ));
    }

    private boolean isAllowed(
            LockedCommand command,
            String userId,
            Instant approvedAt,
            LocalDate calendarDate
    ) {
        if (command.executionPolicy() == CommandExecutionPolicy.USER_CALENDAR_DAY) {
            return !executionPort.existsCalendarDate(command.id(), userId, calendarDate);
        }
        Integer cooldownSeconds = command.userCooldownSeconds();
        if (cooldownSeconds == null) {
            throw new IllegalStateException("Interval command requires userCooldownSeconds");
        }
        Optional<ExecutionRecord> latest = executionPort.findLatestForUpdate(command.id(), userId);
        return latest.isEmpty()
                || !approvedAt.isBefore(latest.get().executedAt().plusSeconds(cooldownSeconds));
    }

    private Streaks streaks(List<LocalDate> executionDates, LocalDate today) {
        List<LocalDate> dates = new ArrayList<>(executionDates.stream().distinct().toList());
        dates.sort(Comparator.naturalOrder());
        if (dates.isEmpty()) {
            return new Streaks(0, 0);
        }
        int longest = 1;
        int running = 1;
        for (int index = 1; index < dates.size(); index++) {
            if (dates.get(index).equals(dates.get(index - 1).plusDays(1))) {
                running++;
                longest = Math.max(longest, running);
            } else {
                running = 1;
            }
        }

        LocalDate latest = dates.getLast();
        if (latest.isBefore(today.minusDays(1))) {
            return new Streaks(0, longest);
        }
        int current = 1;
        for (int index = dates.size() - 1; index > 0; index--) {
            if (!dates.get(index - 1).equals(dates.get(index).minusDays(1))) {
                break;
            }
            current++;
        }
        return new Streaks(current, longest);
    }

    private record Streaks(int current, int longest) {
    }
}
