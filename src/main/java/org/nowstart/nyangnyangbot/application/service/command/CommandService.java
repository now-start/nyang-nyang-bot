package org.nowstart.nyangnyangbot.application.service.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.CommandRecord;
import org.nowstart.nyangnyangbot.application.validation.UseCaseValidator;
import org.nowstart.nyangnyangbot.domain.chat.CommandTrigger;
import org.nowstart.nyangnyangbot.domain.command.CommandExecutionPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class CommandService implements ManageCommandUseCase {

    public static final int DEFAULT_USER_COOLDOWN_SECONDS = 30;
    private static final int MIN_USER_COOLDOWN_SECONDS = 5;
    private static final int MAX_USER_COOLDOWN_SECONDS = 3_600;

    private final CommandPort commandPort;
    private final CommandTemplateRenderer templateRenderer;
    private final CommandVariableRegistry variableRegistry;
    private final UseCaseValidator useCaseValidator;

    @Override
    @Transactional(readOnly = true)
    public List<CommandResult> getCommands() {
        return commandPort.findAllOrderByIdDesc().stream()
                .map(this::commandResult)
                .toList();
    }

    @Override
    public List<VariableResult> getVariables() {
        return variableRegistry.definitions().stream()
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
    public CommandResult createCommand(CreateCommand request) {
        ValidationState state = validationForCreate(request);
        requireValid(state);
        String actor = actor(request.actorId());
        CommandRecord saved = commandPort.create(new CommandPort.CreateData(
                state.trigger(),
                state.messageTemplate(),
                Boolean.TRUE.equals(request.active()),
                state.executionPolicy(),
                state.userCooldownSeconds(),
                actor,
                actor
        ));
        log.info("level=AUDIT action=command.create result=success commandId={} trigger={}",
                saved.id(), saved.trigger());
        return commandResult(saved);
    }

    @Override
    @Transactional
    public CommandResult updateCommand(Long commandId, UpdateCommand request) {
        if (request == null) {
            throw new IllegalArgumentException("command is required");
        }
        CommandRecord current = commandPort.findByIdForUpdate(commandId)
                .orElseThrow(() -> new IllegalArgumentException("command not found"));
        String trigger = request.trigger() == null ? current.trigger() : request.trigger();
        String template = request.messageTemplate() == null
                ? current.messageTemplate()
                : request.messageTemplate();
        CommandExecutionPolicy executionPolicy = request.executionPolicy() == null
                ? current.executionPolicy()
                : request.executionPolicy();
        Integer cooldown = resolveUpdateCooldown(request, current, executionPolicy);
        ValidationState state = validationForRequest(
                commandId,
                trigger,
                template,
                executionPolicy,
                cooldown,
                useCaseValidator.errors(request)
        );
        requireValid(state);
        CommandRecord saved = commandPort.update(new CommandPort.UpdateData(
                commandId,
                state.trigger(),
                state.messageTemplate(),
                request.active() == null ? current.active() : request.active(),
                state.executionPolicy(),
                state.userCooldownSeconds(),
                actor(request.actorId())
        ));
        log.info("level=AUDIT action=command.update result=success commandId={} trigger={}",
                saved.id(), saved.trigger());
        return commandResult(saved);
    }

    @Override
    public PreviewResult preview(PreviewCommand request) {
        if (request == null) {
            throw new IllegalArgumentException("preview is required");
        }
        List<String> errors = new ArrayList<>(useCaseValidator.errors(request));
        String template = cleanTemplate(request.messageTemplate());
        errors.addAll(templateErrors(template));
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors.stream().distinct().toList()));
        }
        Set<String> variables = templateRenderer.variables(template);
        return new PreviewResult(templateRenderer.render(template, variableRegistry.sampleValues(variables)));
    }

    @Override
    @Transactional(readOnly = true)
    public ValidationResult validate(ValidateCommand request) {
        if (request == null) {
            return new ValidationResult(false, List.of("command is required"));
        }
        ValidationState state = validationForRequest(
                request.commandId(),
                request.trigger(),
                request.messageTemplate(),
                request.executionPolicy(),
                request.userCooldownSeconds(),
                useCaseValidator.errors(request)
        );
        return new ValidationResult(state.errors().isEmpty(), state.errors());
    }

    private ValidationState validationForCreate(CreateCommand request) {
        if (request == null) {
            throw new IllegalArgumentException("command is required");
        }
        return validationForRequest(
                null,
                request.trigger(),
                request.messageTemplate(),
                request.executionPolicy(),
                request.userCooldownSeconds(),
                useCaseValidator.errors(request)
        );
    }

    private ValidationState validationForRequest(
            Long commandId,
            String triggerValue,
            String messageTemplateValue,
            CommandExecutionPolicy executionPolicyValue,
            Integer userCooldownSecondsValue,
            List<String> initialErrors
    ) {
        List<String> errors = new ArrayList<>(initialErrors);
        String trigger = CommandTrigger.normalize(triggerValue);
        String template = cleanTemplate(messageTemplateValue);
        CommandExecutionPolicy executionPolicy = executionPolicyValue == null
                ? CommandExecutionPolicy.USER_INTERVAL
                : executionPolicyValue;
        Integer cooldown = executionPolicy == CommandExecutionPolicy.USER_CALENDAR_DAY
                ? null
                : userCooldownSecondsValue == null ? DEFAULT_USER_COOLDOWN_SECONDS : userCooldownSecondsValue;

        errors.addAll(CommandTrigger.validationErrors(trigger));
        errors.addAll(templateErrors(template));
        if (executionPolicy == CommandExecutionPolicy.USER_INTERVAL
                && (cooldown < MIN_USER_COOLDOWN_SECONDS || cooldown > MAX_USER_COOLDOWN_SECONDS)) {
            errors.add("userCooldownSeconds must be between 5 and 3600");
        }
        if (trigger != null) {
            commandPort.findByTrigger(trigger).ifPresent(existing -> {
                if (!existing.id().equals(commandId)) {
                    errors.add("trigger already exists");
                }
            });
        }
        return new ValidationState(trigger, template, executionPolicy, cooldown, errors.stream().distinct().toList());
    }

    private List<String> templateErrors(String template) {
        List<String> errors = new ArrayList<>();
        if (template == null || template.isBlank()) {
            errors.add("messageTemplate is required");
            return errors;
        }
        if (template.length() > ManageCommandUseCase.MAX_TEMPLATE_LENGTH) {
            errors.add("messageTemplate length must be "
                    + ManageCommandUseCase.MAX_TEMPLATE_LENGTH + " or less");
        }
        Set<String> malformed = templateRenderer.malformedVariables(template);
        if (!malformed.isEmpty()) {
            errors.add("malformed template variables: " + String.join(", ", malformed));
        }
        Set<String> unknown = variableRegistry.unknownVariables(templateRenderer.variables(template));
        if (!unknown.isEmpty()) {
            errors.add("unknown template variables: " + String.join(", ", unknown));
        }
        return errors;
    }

    private void requireValid(ValidationState state) {
        if (!state.errors().isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", state.errors()));
        }
    }

    private CommandResult commandResult(CommandRecord command) {
        return new CommandResult(
                command.id(),
                command.trigger(),
                command.messageTemplate(),
                command.active(),
                command.executionPolicy(),
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
        return value == null || value.isBlank() || "system".equals(value) ? null : value;
    }

    private Integer resolveUpdateCooldown(
            UpdateCommand request,
            CommandRecord current,
            CommandExecutionPolicy executionPolicy
    ) {
        if (executionPolicy == CommandExecutionPolicy.USER_CALENDAR_DAY) {
            return null;
        }
        if (request.userCooldownSeconds() != null) {
            return request.userCooldownSeconds();
        }
        return current.userCooldownSeconds() == null
                ? DEFAULT_USER_COOLDOWN_SECONDS
                : current.userCooldownSeconds();
    }

    private record ValidationState(
            String trigger,
            String messageTemplate,
            CommandExecutionPolicy executionPolicy,
            Integer userCooldownSeconds,
            List<String> errors
    ) {
    }
}
