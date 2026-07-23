package org.nowstart.nyangnyangbot.application.service.timer;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase;
import org.nowstart.nyangnyangbot.application.port.in.timer.RecordTimerChatUseCase;
import org.nowstart.nyangnyangbot.application.port.in.timer.RunTimerMessagesUseCase;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.MessageCommand;
import org.nowstart.nyangnyangbot.application.port.out.timer.TimerMessagePort;
import org.nowstart.nyangnyangbot.application.port.out.timer.TimerMessagePort.ClaimedTimerMessage;
import org.nowstart.nyangnyangbot.application.port.out.timer.TimerMessagePort.TimerMessageRecord;
import org.nowstart.nyangnyangbot.application.service.command.CommandTemplateRenderer;
import org.nowstart.nyangnyangbot.application.service.command.CommandVariableContext;
import org.nowstart.nyangnyangbot.application.service.command.CommandVariableRegistry;
import org.nowstart.nyangnyangbot.application.validation.UseCaseValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class TimerMessageService implements ManageTimerMessageUseCase, RecordTimerChatUseCase, RunTimerMessagesUseCase {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_INTERVAL_MINUTES = 30;
    private static final int DEFAULT_MIN_CHAT_COUNT = 10;
    private static final int MIN_INTERVAL_MINUTES = 5;
    private static final int MAX_INTERVAL_MINUTES = 1_440;
    private static final int MIN_CHAT_COUNT = 1;
    private static final int MAX_CHAT_COUNT = 10_000;
    private static final int CLAIM_BATCH_SIZE = 100;

    private final TimerMessagePort timerMessagePort;
    private final ChzzkClientPort chzzkClientPort;
    private final CommandTemplateRenderer templateRenderer;
    private final CommandVariableRegistry variableRegistry;
    private final UseCaseValidator useCaseValidator;

    @Override
    @Transactional(readOnly = true)
    public List<TimerMessageResult> getTimerMessages() {
        return timerMessagePort.findAllOrderByIdDesc().stream()
                .map(this::result)
                .toList();
    }

    @Override
    public List<VariableResult> getVariables() {
        return variableRegistry.definitions().stream()
                .filter(definition -> isTimerVariable(definition.key()))
                .map(definition -> new VariableResult(
                        definition.key(),
                        definition.label(),
                        definition.description(),
                        definition.example()
                ))
                .toList();
    }

    @Override
    @Transactional
    public TimerMessageResult createTimerMessage(CreateTimerMessage request) {
        if (request == null) {
            throw new IllegalArgumentException("timerMessage is required");
        }
        ValidationState state = validateState(
                request.messageTemplate(),
                request.intervalMinutes(),
                request.minChatCount(),
                useCaseValidator.errors(request)
        );
        requireValid(state);
        boolean active = Boolean.TRUE.equals(request.active());
        LocalDateTime nextRunAt = active ? currentDateTime().plusMinutes(state.intervalMinutes()) : null;
        String actor = actor(request.actorId());
        TimerMessageRecord saved = timerMessagePort.create(new TimerMessagePort.CreateData(
                state.messageTemplate(),
                state.intervalMinutes(),
                state.minChatCount(),
                active,
                nextRunAt,
                actor,
                actor
        ));
        log.info("level=AUDIT action=timer-message.create result=success timerMessageId={}", saved.id());
        return result(saved);
    }

    @Override
    @Transactional
    public TimerMessageResult updateTimerMessage(Long timerMessageId, UpdateTimerMessage request) {
        if (request == null) {
            throw new IllegalArgumentException("timerMessage is required");
        }
        TimerMessageRecord current = timerMessagePort.findByIdForUpdate(timerMessageId)
                .orElseThrow(() -> new IllegalArgumentException("timer message not found"));
        String template = request.messageTemplate() == null ? current.messageTemplate() : request.messageTemplate();
        Integer interval = request.intervalMinutes() == null ? current.intervalMinutes() : request.intervalMinutes();
        Integer minChatCount = request.minChatCount() == null ? current.minChatCount() : request.minChatCount();
        ValidationState state = validateState(
                template,
                interval,
                minChatCount,
                useCaseValidator.errors(request)
        );
        requireValid(state);
        boolean active = request.active() == null ? current.active() : request.active();
        boolean resetSchedule = active != current.active()
                || (active && !state.intervalMinutes().equals(current.intervalMinutes()));
        LocalDateTime nextRunAt;
        if (!active) {
            nextRunAt = null;
            resetSchedule = true;
        } else if (resetSchedule || current.nextRunAt() == null) {
            nextRunAt = currentDateTime().plusMinutes(state.intervalMinutes());
            resetSchedule = true;
        } else {
            nextRunAt = current.nextRunAt();
        }
        TimerMessageRecord saved = timerMessagePort.update(new TimerMessagePort.UpdateData(
                timerMessageId,
                state.messageTemplate(),
                state.intervalMinutes(),
                state.minChatCount(),
                active,
                nextRunAt,
                resetSchedule,
                actor(request.actorId())
        ));
        log.info("level=AUDIT action=timer-message.update result=success timerMessageId={}", saved.id());
        return result(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PreviewResult preview(PreviewTimerMessage request) {
        if (request == null) {
            throw new IllegalArgumentException("preview is required");
        }
        ValidationState state = validateState(
                request.messageTemplate(),
                DEFAULT_INTERVAL_MINUTES,
                DEFAULT_MIN_CHAT_COUNT,
                useCaseValidator.errors(request)
        );
        requireValid(state);
        Set<String> requestedVariables = templateRenderer.variables(state.messageTemplate());
        String rendered = templateRenderer.render(
                state.messageTemplate(),
                variableRegistry.resolve(requestedVariables, timerContext(currentDateTime()))
        );
        return new PreviewResult(rendered);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidationResult validate(ValidateTimerMessage request) {
        if (request == null) {
            return new ValidationResult(false, List.of("timerMessage is required"));
        }
        ValidationState state = validateState(
                request.messageTemplate(),
                request.intervalMinutes(),
                request.minChatCount(),
                List.of()
        );
        return new ValidationResult(state.errors().isEmpty(), state.errors());
    }

    @Override
    @Transactional
    public void recordChatActivity() {
        timerMessagePort.incrementActiveChatCounts();
    }

    @Override
    public void runDueTimerMessages() {
        LocalDateTime scanTime = currentDateTime();
        List<Long> candidateIds = timerMessagePort.findClaimCandidateIds(scanTime, CLAIM_BATCH_SIZE);
        for (Long candidateId : candidateIds) {
            runCandidate(candidateId);
        }
    }

    private void runCandidate(Long timerMessageId) {
        LocalDateTime claimTime = currentDateTime();
        String claimToken = newClaimToken();
        var claimed = timerMessagePort.claimDue(
                timerMessageId,
                claimToken,
                claimTime,
                claimTime.plusMinutes(2)
        );
        if (claimed.isEmpty()) {
            return;
        }
        ClaimedTimerMessage timer = claimed.get();
        try {
            Set<String> variables = templateRenderer.variables(timer.messageTemplate());
            String message = templateRenderer.render(
                    timer.messageTemplate(),
                    variableRegistry.resolve(variables, timerContext(claimTime))
            );
            if (message.isBlank()) {
                throw new IllegalStateException("timer message rendered blank");
            }
            chzzkClientPort.sendMessage(new MessageCommand(message));
            LocalDateTime sentAt = currentDateTime();
            boolean completed = timerMessagePort.completeClaim(
                    timer.id(),
                    timer.claimToken(),
                    timer.claimedNextRunAt(),
                    timer.intervalMinutes(),
                    sentAt,
                    sentAt.plusMinutes(timer.intervalMinutes())
            );
            if (!completed) {
                log.warn("Timer message was sent but claim completion was skipped: timerMessageId={}", timer.id());
                return;
            }
            log.info("level=AUDIT action=timer-message.send result=success timerMessageId={}", timer.id());
        } catch (RuntimeException e) {
            LocalDateTime failedAt = currentDateTime();
            boolean released = timerMessagePort.releaseClaim(
                    timer.id(),
                    timer.claimToken(),
                    timer.claimedNextRunAt(),
                    timer.intervalMinutes(),
                    failedAt.plusMinutes(1)
            );
            if (!released) {
                log.warn("Timer message claim release was skipped: timerMessageId={}", timer.id());
            }
            log.warn("Timer message send failed: timerMessageId={}", timer.id(), e);
        }
    }

    private ValidationState validateState(
            String messageTemplate,
            Integer intervalMinutes,
            Integer minChatCount,
            List<String> initialErrors
    ) {
        List<String> errors = new ArrayList<>(initialErrors);
        String template = cleanTemplate(messageTemplate);
        int interval = intervalMinutes == null ? DEFAULT_INTERVAL_MINUTES : intervalMinutes;
        int minimumChats = minChatCount == null ? DEFAULT_MIN_CHAT_COUNT : minChatCount;

        if (template == null || template.isBlank()) {
            errors.add("messageTemplate is required");
        } else {
            if (template.length() > MAX_TEMPLATE_LENGTH) {
                errors.add("messageTemplate length must be 1000 or less");
            }
            Set<String> malformed = templateRenderer.malformedVariables(template);
            if (!malformed.isEmpty()) {
                errors.add("malformed template variables: " + String.join(", ", malformed));
            }
            Set<String> variables = templateRenderer.variables(template);
            Set<String> unknown = variableRegistry.unknownVariables(variables);
            if (!unknown.isEmpty()) {
                errors.add("unknown template variables: " + String.join(", ", unknown));
            }
            List<String> forbidden = variables.stream()
                    .filter(variable -> !unknown.contains(variable))
                    .filter(variable -> !isTimerVariable(variable))
                    .sorted()
                    .toList();
            if (!forbidden.isEmpty()) {
                errors.add("timer messageTemplate cannot use variables: " + String.join(", ", forbidden));
            }
        }
        if (interval < MIN_INTERVAL_MINUTES || interval > MAX_INTERVAL_MINUTES) {
            errors.add("intervalMinutes must be between 5 and 1440");
        }
        if (minimumChats < MIN_CHAT_COUNT || minimumChats > MAX_CHAT_COUNT) {
            errors.add("minChatCount must be between 1 and 10000");
        }
        return new ValidationState(template, interval, minimumChats, errors.stream().distinct().toList());
    }

    private void requireValid(ValidationState state) {
        if (!state.errors().isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", state.errors()));
        }
    }

    private boolean isTimerVariable(String key) {
        return key != null && key.startsWith("time.");
    }

    private CommandVariableContext timerContext(LocalDateTime seoulNow) {
        return new CommandVariableContext(null, null, null, null, null, null, seoulNow);
    }

    private TimerMessageResult result(TimerMessageRecord record) {
        return new TimerMessageResult(
                record.id(),
                record.messageTemplate(),
                record.intervalMinutes(),
                record.minChatCount(),
                record.active(),
                record.chatCountSinceLastSend(),
                record.lastSentAt(),
                record.nextRunAt(),
                record.createdBy(),
                record.updatedBy(),
                record.createDate(),
                record.modifyDate()
        );
    }

    private String cleanTemplate(String value) {
        return value == null ? null : value.trim();
    }

    private String actor(String value) {
        return value == null || value.isBlank() || "system".equals(value) ? null : value;
    }

    LocalDateTime currentDateTime() {
        return LocalDateTime.now(SEOUL);
    }

    String newClaimToken() {
        return UUID.randomUUID().toString();
    }

    private record ValidationState(
            String messageTemplate,
            Integer intervalMinutes,
            Integer minChatCount,
            List<String> errors
    ) {
    }
}
