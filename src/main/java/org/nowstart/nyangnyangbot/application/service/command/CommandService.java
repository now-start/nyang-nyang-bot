package org.nowstart.nyangnyangbot.application.service.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.CommandRecord;
import org.nowstart.nyangnyangbot.application.service.command.CommandTemplateRenderer.TemplateContext;
import org.nowstart.nyangnyangbot.application.validation.UseCaseValidator;
import org.nowstart.nyangnyangbot.domain.type.CommandActionKey;
import org.nowstart.nyangnyangbot.domain.type.CommandType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandService implements ManageCommandUseCase {

    public static final int DEFAULT_USER_COOLDOWN_SECONDS = 30;
    private static final int MAX_TRIGGER_LENGTH = 30;
    private static final int MAX_TEMPLATE_LENGTH = 300;
    private static final int MIN_USER_COOLDOWN_SECONDS = 5;
    private static final int MAX_USER_COOLDOWN_SECONDS = 3_600;
    private static final int MIN_TIMER_INTERVAL_MINUTES = 5;
    private static final int MAX_TIMER_INTERVAL_MINUTES = 1_440;
    private static final int MIN_TIMER_CHAT_COUNT = 1;
    private static final int MAX_TIMER_CHAT_COUNT = 10_000;
    private static final String DEFAULT_ROLE = "USER";
    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "ADMIN");

    private final CommandPort commandPort;
    private final CommandTemplateRenderer templateRenderer;
    private final UseCaseValidator useCaseValidator;

    @Override
    @Transactional(readOnly = true)
    public List<CommandResult> getCommands() {
        return commandPort.findAllOrderByIdDesc().stream()
                .map(this::commandResult)
                .toList();
    }

    @Override
    @Transactional
    public CommandResult createCommand(CreateCommand request) {
        ValidationState state = validationForCreate(request);
        if (!state.errors().isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", state.errors()));
        }
        String actor = actor(request.actorId());
        CommandRecord saved = commandPort.create(new CommandPort.CreateData(
                state.type(),
                state.trigger(),
                state.actionKey(),
                state.messageTemplate(),
                state.timerIntervalMinutes(),
                state.timerMinChatCount(),
                Boolean.TRUE.equals(request.active()),
                state.requiredRole(),
                state.userCooldownSeconds(),
                actor,
                actor
        ));
        log.info("level=AUDIT action=command.create result=success commandId={} type={} trigger={}",
                saved.id(), saved.type(), saved.trigger());
        return commandResult(saved);
    }

    @Override
    @Transactional
    public CommandResult updateCommand(Long commandId, UpdateCommand request) {
        CommandRecord current = commandPort.findById(commandId)
                .orElseThrow(() -> new IllegalArgumentException("command not found"));
        ValidationState state = validationForUpdate(current, request);
        if (!state.errors().isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", state.errors()));
        }
        CommandRecord saved = commandPort.update(new CommandPort.UpdateData(
                commandId,
                state.trigger(),
                state.messageTemplate(),
                state.timerIntervalMinutes(),
                state.timerMinChatCount(),
                request.active() == null ? current.active() : request.active(),
                state.requiredRole(),
                state.userCooldownSeconds(),
                actor(request.actorId())
        ));
        log.info("level=AUDIT action=command.update result=success commandId={} type={} trigger={}",
                saved.id(), saved.type(), saved.trigger());
        return commandResult(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PreviewResult preview(PreviewCommand request) {
        if (request == null) {
            throw new IllegalArgumentException("preview is required");
        }
        useCaseValidator.validate(new PreviewDefinition(request.messageTemplate()), "preview is required");
        String message = templateRenderer.render(
                cleanTemplate(request.messageTemplate()),
                new TemplateContext(
                        fallback(request.nickname(), "치즈냥"),
                        fallback(request.command(), "!명령어"),
                        fallback(request.args(), ""),
                        fallback(request.arg1(), ""),
                        fallback(request.arg2(), ""),
                        request.favorite() == null ? 0 : request.favorite(),
                        LocalDateTime.now()
                )
        );
        return new PreviewResult(message);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidationResult validate(ValidateCommand request) {
        if (request == null) {
            return new ValidationResult(false, List.of("command is required"));
        }
        List<String> errors = validationForRequest(
                request.commandId(),
                request.type(),
                request.trigger(),
                request.actionKey(),
                request.messageTemplate(),
                request.timerIntervalMinutes(),
                request.timerMinChatCount(),
                request.requiredRole(),
                request.userCooldownSeconds()
        ).errors();
        return new ValidationResult(errors.isEmpty(), errors);
    }

    public static String normalizeTrigger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }

    private ValidationState validationForCreate(CreateCommand request) {
        if (request == null) {
            throw new IllegalArgumentException("command is required");
        }
        return validationForRequest(
                null,
                request.type(),
                request.trigger(),
                request.actionKey(),
                request.messageTemplate(),
                request.timerIntervalMinutes(),
                request.timerMinChatCount(),
                request.requiredRole(),
                request.userCooldownSeconds()
        );
    }

    private ValidationState validationForUpdate(CommandRecord current, UpdateCommand request) {
        if (request == null) {
            throw new IllegalArgumentException("command is required");
        }
        List<String> errors = new ArrayList<>();
        if (request.type() != null && !request.type().isBlank()) {
            CommandType requestedType = parseTypeOrNull(request.type());
            if (requestedType == null) {
                errors.add("type is invalid");
            } else if (requestedType != current.type()) {
                errors.add("type cannot be changed");
            }
        }
        if (request.actionKey() != null && !request.actionKey().isBlank()) {
            CommandActionKey requestedActionKey = parseActionKeyOrNull(request.actionKey());
            if (requestedActionKey == null) {
                errors.add("actionKey is invalid");
            } else if (requestedActionKey != current.actionKey()) {
                errors.add("actionKey cannot be changed");
            }
        }
        ValidationState merged = validationForRequest(
                current.id(),
                current.type().name(),
                request.trigger() == null ? current.trigger() : request.trigger(),
                current.actionKey() == null ? null : current.actionKey().name(),
                request.messageTemplate() == null ? current.messageTemplate() : request.messageTemplate(),
                request.timerIntervalMinutes() == null
                        ? current.timerIntervalMinutes()
                        : request.timerIntervalMinutes(),
                request.timerMinChatCount() == null ? current.timerMinChatCount() : request.timerMinChatCount(),
                request.requiredRole() == null ? current.requiredRole() : request.requiredRole(),
                request.userCooldownSeconds() == null
                        ? current.userCooldownSeconds()
                        : request.userCooldownSeconds()
        );
        errors.addAll(merged.errors());
        return new ValidationState(
                merged.type(),
                merged.trigger(),
                merged.actionKey(),
                merged.messageTemplate(),
                merged.timerIntervalMinutes(),
                merged.timerMinChatCount(),
                merged.requiredRole(),
                merged.userCooldownSeconds(),
                errors
        );
    }

    private ValidationState validationForRequest(
            Long commandId,
            String typeValue,
            String triggerValue,
            String actionKeyValue,
            String messageTemplateValue,
            Integer timerIntervalMinutesValue,
            Integer timerMinChatCountValue,
            String requiredRoleValue,
            Integer userCooldownSecondsValue
    ) {
        List<String> errors = new ArrayList<>();
        CommandType type = parseType(typeValue, errors);
        String trigger = normalizeTrigger(triggerValue);
        CommandActionKey actionKey = parseActionKey(type, actionKeyValue, errors);
        String role = role(requiredRoleValue, errors);
        Integer cooldown = cooldown(actionKey, userCooldownSecondsValue, errors);
        String template = cleanTemplate(messageTemplateValue);
        Integer timerIntervalMinutes = timerIntervalMinutesValue;
        Integer timerMinChatCount = timerMinChatCountValue;

        if (type != null) {
            validateTrigger(type, trigger, errors);
            validateTemplate(type, template, errors);
            validateTimer(type, timerIntervalMinutes, timerMinChatCount, errors);
            if (type != CommandType.TIMER) {
                timerIntervalMinutes = null;
                timerMinChatCount = null;
            } else if (timerMinChatCount == null) {
                timerMinChatCount = MIN_TIMER_CHAT_COUNT;
            }
            if (type != CommandType.TRIGGER && actionKey != null) {
                errors.add("actionKey is only allowed for TRIGGER");
            }
        }

        if (trigger != null) {
            validateDuplicateTrigger(commandId, trigger, errors);
        }
        if (actionKey != null) {
            validateDuplicateActionKey(commandId, actionKey, errors);
        }
        return new ValidationState(
                type,
                trigger,
                actionKey,
                template,
                timerIntervalMinutes,
                timerMinChatCount,
                role,
                cooldown,
                errors
        );
    }

    private CommandType parseType(String value, List<String> errors) {
        CommandType type = parseTypeOrNull(value);
        if (type == null) {
            errors.add(value == null || value.isBlank() ? "type is required" : "type is invalid");
        }
        return type;
    }

    private CommandType parseTypeOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return CommandType.parse(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private CommandActionKey parseActionKey(
            CommandType type,
            String value,
            List<String> errors
    ) {
        if (type != CommandType.TRIGGER && (value == null || value.isBlank())) {
            return null;
        }
        if (type == CommandType.TRIGGER && (value == null || value.isBlank())) {
            errors.add("actionKey is required");
            return null;
        }
        CommandActionKey actionKey = parseActionKeyOrNull(value);
        if (actionKey == null) {
            errors.add("actionKey is invalid");
        }
        return actionKey;
    }

    private CommandActionKey parseActionKeyOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return CommandActionKey.parse(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void validateTrigger(CommandType type, String trigger, List<String> errors) {
        if (type != CommandType.TIMER && trigger == null) {
            errors.add("trigger is required");
            return;
        }
        if (trigger == null) {
            return;
        }
        if (!trigger.startsWith("!")) {
            errors.add("trigger must start with !");
        }
        if (trigger.length() < 2 || trigger.length() > MAX_TRIGGER_LENGTH) {
            errors.add("trigger length must be between 2 and " + MAX_TRIGGER_LENGTH);
        }
        if (trigger.matches(".*\\s+.*")) {
            errors.add("trigger must be a single token");
        }
    }

    private void validateTemplate(CommandType type, String template, List<String> errors) {
        if (type == CommandType.TRIGGER) {
            return;
        }
        errors.addAll(templateErrors(template));
    }

    private List<String> templateErrors(String template) {
        List<String> errors = new ArrayList<>();
        if (template == null || template.isBlank()) {
            errors.add("messageTemplate is required");
            return errors;
        }
        if (template.length() > MAX_TEMPLATE_LENGTH) {
            errors.add("messageTemplate length must be " + MAX_TEMPLATE_LENGTH + " or less");
        }
        Set<String> unknown = CommandTemplateRenderer.unknownVariables(template);
        if (!unknown.isEmpty()) {
            errors.add("unknown template variables: " + String.join(", ", unknown));
        }
        return errors;
    }

    private void validateTimer(
            CommandType type,
            Integer timerIntervalMinutes,
            Integer timerMinChatCount,
            List<String> errors
    ) {
        if (type != CommandType.TIMER) {
            return;
        }
        if (timerIntervalMinutes == null
                || timerIntervalMinutes < MIN_TIMER_INTERVAL_MINUTES
                || timerIntervalMinutes > MAX_TIMER_INTERVAL_MINUTES) {
            errors.add("timerIntervalMinutes must be between 5 and 1440");
        }
        if (timerMinChatCount != null
                && (timerMinChatCount < MIN_TIMER_CHAT_COUNT || timerMinChatCount > MAX_TIMER_CHAT_COUNT)) {
            errors.add("timerMinChatCount must be between 1 and 10000");
        }
    }

    private void validateDuplicateTrigger(Long commandId, String trigger, List<String> errors) {
        commandPort.findByTrigger(trigger).ifPresent(existing -> {
            if (!existing.id().equals(commandId)) {
                errors.add("trigger already exists");
            }
        });
    }

    private String role(String value, List<String> errors) {
        String role = value == null || value.isBlank()
                ? DEFAULT_ROLE
                : value.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(role)) {
            errors.add("requiredRole is invalid");
        }
        return role;
    }

    private Integer cooldown(CommandActionKey actionKey, Integer value, List<String> errors) {
        if (value == null) {
            return DEFAULT_USER_COOLDOWN_SECONDS;
        }
        if (actionKey == CommandActionKey.ROULETTE_DONATION && value == 0) {
            return value;
        }
        if (value < MIN_USER_COOLDOWN_SECONDS || value > MAX_USER_COOLDOWN_SECONDS) {
            errors.add("userCooldownSeconds must be between 5 and 3600");
        }
        return value;
    }

    private void validateDuplicateActionKey(
            Long commandId,
            CommandActionKey actionKey,
            List<String> errors
    ) {
        commandPort.findByActionKey(actionKey).ifPresent(existing -> {
            if (!existing.id().equals(commandId)) {
                errors.add("actionKey already exists");
            }
        });
    }

    private CommandResult commandResult(CommandRecord command) {
        return new CommandResult(
                command.id(),
                command.type().name(),
                command.trigger(),
                command.actionKey() == null ? null : command.actionKey().name(),
                command.messageTemplate(),
                command.timerIntervalMinutes(),
                command.timerMinChatCount(),
                command.active(),
                command.requiredRole(),
                command.userCooldownSeconds(),
                command.createdBy(),
                command.updatedBy(),
                command.createDate(),
                command.modifyDate()
        );
    }

    private String cleanTemplate(String value) {
        return value == null ? null : value.trim();
    }

    private String actor(String value) {
        return value == null || value.isBlank() ? "system" : value;
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record ValidationState(
            CommandType type,
            String trigger,
            CommandActionKey actionKey,
            String messageTemplate,
            Integer timerIntervalMinutes,
            Integer timerMinChatCount,
            String requiredRole,
            Integer userCooldownSeconds,
            List<String> errors
    ) {
    }

    private record PreviewDefinition(
            @NotBlank(message = "messageTemplate is required")
            @Size(max = 300, message = "messageTemplate length must be 300 or less")
            String messageTemplate
    ) {
    }
}
