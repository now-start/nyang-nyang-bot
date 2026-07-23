package org.nowstart.nyangnyangbot.adapter.in.web.command;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.CommandResult;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.CreateCommand;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.PreviewCommand;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.UpdateCommand;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.ValidateCommand;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.VariableResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/commands")
@PreAuthorize("hasRole('ADMIN')")
public class CommandController {

    private static final String COMMAND_LIST_FRAGMENT = "features/command/regions :: command-list-region";
    private static final String COMMAND_EDITOR_FRAGMENT = "features/command/regions :: command-editor-region";
    private static final String COMMAND_REVIEW_FRAGMENT = "features/command/regions :: command-review-region";
    private static final String DEFAULT_ACTOR = "system";
    private static final String COMMAND_LIST_REFRESH_TRIGGER = "command-list-refresh";
    private static final String COMMAND_NOT_FOUND_MESSAGE = "명령어를 찾을 수 없습니다.";

    private final ManageCommandUseCase manageCommandUseCase;

    @GetMapping
    public String list(@RequestParam(required = false) Boolean active, Model model) {
        model.addAttribute("commands", commandViews(active));
        return COMMAND_LIST_FRAGMENT;
    }

    @GetMapping("/editor")
    public String editor(@RequestParam(required = false) Long commandId, Model model) {
        if (commandId == null) {
            return COMMAND_EDITOR_FRAGMENT;
        }
        try {
            model.addAttribute("commandForm", commandForm(commandId));
            addCommandVariables(model);
        } catch (IllegalArgumentException e) {
            model.addAttribute("saveError", inputErrorMessage(e));
        }
        return COMMAND_EDITOR_FRAGMENT;
    }

    @GetMapping("/editor/new")
    public String newEditor(Model model) {
        model.addAttribute("commandForm", CommandForm.empty());
        addCommandVariables(model);
        return COMMAND_EDITOR_FRAGMENT;
    }

    @PostMapping("/preview")
    public String preview(@ModelAttribute CommandForm form, Model model) {
        CommandForm normalizedForm = form.withDefaults();
        var validation = manageCommandUseCase.validate(validateCommand(normalizedForm));
        if (!validation.valid()) {
            model.addAttribute("reviewResult", new ReviewView(false, validation.errors(), null));
            return COMMAND_REVIEW_FRAGMENT;
        }
        try {
            var result = manageCommandUseCase.preview(new PreviewCommand(normalizedForm.messageTemplate()));
            model.addAttribute("reviewResult", new ReviewView(true, List.of(), result.message()));
        } catch (IllegalArgumentException | ConstraintViolationException e) {
            model.addAttribute("reviewResult", new ReviewView(false, List.of(inputErrorMessage(e)), null));
        }
        return COMMAND_REVIEW_FRAGMENT;
    }

    @PostMapping
    public String save(
            @ModelAttribute CommandForm form,
            @RequestParam(defaultValue = "false") boolean active,
            Authentication authentication,
            HttpServletResponse response,
            Model model
    ) {
        CommandForm activeForm = form.withActive(active).withDefaults();
        addCommandVariables(model);
        try {
            CommandResult result = activeForm.commandId() == null
                    ? manageCommandUseCase.createCommand(createCommand(activeForm, actor(authentication)))
                    : manageCommandUseCase.updateCommand(
                            activeForm.commandId(),
                            updateCommand(activeForm, actor(authentication))
                    );
            model.addAttribute("commandForm", CommandForm.from(result));
            model.addAttribute("saveMessage", "저장됨");
            response.addHeader("HX-Trigger", COMMAND_LIST_REFRESH_TRIGGER);
        } catch (IllegalArgumentException | ConstraintViolationException e) {
            model.addAttribute("commandForm", activeForm);
            model.addAttribute("saveError", inputErrorMessage(e));
        }
        return COMMAND_EDITOR_FRAGMENT;
    }

    @PostMapping("/deactivate")
    public String deactivate(
            @RequestParam(required = false) Long commandId,
            Authentication authentication,
            HttpServletResponse response,
            Model model
    ) {
        addCommandVariables(model);
        if (commandId == null) {
            model.addAttribute("saveError", "저장된 명령어만 비활성화할 수 있습니다.");
            return COMMAND_EDITOR_FRAGMENT;
        }
        try {
            CommandResult result = manageCommandUseCase.updateCommand(
                    commandId,
                    new UpdateCommand(null, null, false, null, actor(authentication))
            );
            model.addAttribute("commandForm", CommandForm.from(result));
            model.addAttribute("saveMessage", "비활성화됨");
            response.addHeader("HX-Trigger", COMMAND_LIST_REFRESH_TRIGGER);
        } catch (IllegalArgumentException | ConstraintViolationException e) {
            model.addAttribute("saveError", inputErrorMessage(e));
        }
        return COMMAND_EDITOR_FRAGMENT;
    }

    private List<CommandView> commandViews(Boolean active) {
        return manageCommandUseCase.getCommands().stream()
                .filter(command -> active == null || active == command.active())
                .map(CommandView::from)
                .toList();
    }

    private void addCommandVariables(Model model) {
        List<VariableResult> variables = manageCommandUseCase.getVariables();
        model.addAttribute("commandVariableGroups", List.of(
                        variableGroup("viewer", "시청자", variables),
                        variableGroup("invocation", "명령 입력", variables),
                        variableGroup("count", "실행 횟수", variables),
                        variableGroup("streak", "연속 실행", variables),
                        variableGroup("time", "시간", variables),
                        integrationVariableGroup(variables)
                ).stream()
                .filter(group -> !group.variables().isEmpty())
                .toList());
    }

    private VariableGroupView variableGroup(String key, String label, List<VariableResult> variables) {
        return new VariableGroupView(
                key,
                label,
                variables.stream()
                        .filter(variable -> variable.key().startsWith(key + "."))
                        .toList()
        );
    }

    private VariableGroupView integrationVariableGroup(List<VariableResult> variables) {
        return new VariableGroupView(
                "integration",
                "연동 데이터",
                variables.stream()
                        .filter(variable -> !variable.key().startsWith("viewer."))
                        .filter(variable -> !variable.key().startsWith("invocation."))
                        .filter(variable -> !variable.key().startsWith("count."))
                        .filter(variable -> !variable.key().startsWith("streak."))
                        .filter(variable -> !variable.key().startsWith("time."))
                        .toList()
        );
    }

    private String inputErrorMessage(RuntimeException exception) {
        if (exception instanceof ConstraintViolationException violationException) {
            return violationException.getConstraintViolations().stream()
                    .map(violation -> violation.getMessage())
                    .sorted()
                    .distinct()
                    .collect(java.util.stream.Collectors.joining(", "));
        }
        return "command not found".equals(exception.getMessage())
                ? COMMAND_NOT_FOUND_MESSAGE
                : exception.getMessage();
    }

    private CommandForm commandForm(Long commandId) {
        return manageCommandUseCase.getCommands().stream()
                .filter(command -> commandId.equals(command.id()))
                .findFirst()
                .map(CommandForm::from)
                .orElseThrow(() -> new IllegalArgumentException(COMMAND_NOT_FOUND_MESSAGE));
    }

    private CreateCommand createCommand(CommandForm form, String actor) {
        return new CreateCommand(
                form.trigger(),
                form.messageTemplate(),
                form.active(),
                ManageCommandUseCase.executionPolicy(form.executionPolicy()),
                form.userCooldownSeconds(),
                actor
        );
    }

    private UpdateCommand updateCommand(CommandForm form, String actor) {
        return new UpdateCommand(
                form.trigger(),
                form.messageTemplate(),
                form.active(),
                ManageCommandUseCase.executionPolicy(form.executionPolicy()),
                form.userCooldownSeconds(),
                actor
        );
    }

    private ValidateCommand validateCommand(CommandForm form) {
        return new ValidateCommand(
                form.commandId(),
                form.trigger(),
                form.messageTemplate(),
                ManageCommandUseCase.executionPolicy(form.executionPolicy()),
                form.userCooldownSeconds()
        );
    }

    private String actor(Authentication authentication) {
        return authentication == null ? DEFAULT_ACTOR : authentication.getName();
    }

    public record OptionView(String value, String label) {
    }

    public record CommandForm(
            Long commandId,
            String trigger,
            String messageTemplate,
            Boolean active,
            String executionPolicy,
            Integer userCooldownSeconds
    ) {

        public CommandForm(
                Long commandId,
                String trigger,
                String messageTemplate,
                Boolean active,
                Integer userCooldownSeconds
        ) {
            this(commandId, trigger, messageTemplate, active, "USER_INTERVAL",
                    userCooldownSeconds);
        }

        static CommandForm empty() {
            return new CommandForm(null, "", "", false, "USER_INTERVAL", 30);
        }

        static CommandForm from(CommandResult result) {
            return new CommandForm(
                    result.id(),
                    result.trigger(),
                    result.messageTemplate(),
                    result.active(),
                    result.executionPolicy().name(),
                    result.userCooldownSeconds()
            ).withDefaults();
        }

        CommandForm deactivate() {
            return new CommandForm(
                    commandId,
                    trigger,
                    messageTemplate,
                    false,
                    executionPolicy,
                    userCooldownSeconds
            ).withDefaults();
        }

        CommandForm withActive(boolean active) {
            return new CommandForm(
                    commandId,
                    trigger,
                    messageTemplate,
                    active,
                    executionPolicy,
                    userCooldownSeconds
            ).withDefaults();
        }

        CommandForm withDefaults() {
            return new CommandForm(
                    commandId,
                    trigger == null ? "" : trigger,
                    messageTemplate == null ? "" : messageTemplate,
                    active != null && active,
                    executionPolicy == null ? "USER_INTERVAL" : executionPolicy,
                    "USER_CALENDAR_DAY".equals(executionPolicy)
                            ? null
                            : userCooldownSeconds == null ? 30 : userCooldownSeconds
            );
        }
    }

    public record CommandView(
            Long id,
            String triggerLabel,
            String messageSummary,
            boolean active,
            String statusLabel,
            String cooldownLabel,
            String updatedByLabel
    ) {

        static CommandView from(CommandResult result) {
            String triggerLabel = result.trigger() == null || result.trigger().isBlank() ? "-" : result.trigger();
            String messageSummary = result.messageTemplate() == null || result.messageTemplate().isBlank()
                    ? "응답 없음"
                    : result.messageTemplate();
            if (messageSummary.length() > 60) {
                messageSummary = messageSummary.substring(0, 57) + "...";
            }
            return new CommandView(
                    result.id(),
                    triggerLabel,
                    messageSummary,
                    result.active(),
                    result.active() ? "활성" : "비활성",
                    "USER_CALENDAR_DAY".equals(result.executionPolicy().name())
                            ? "하루 1회"
                            : result.userCooldownSeconds() + "초",
                    defaultString(result.updatedBy(), defaultString(result.createdBy(), "-"))
            );
        }

        private static String defaultString(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value;
        }
    }

    public record ReviewView(boolean valid, List<String> errors, String previewMessage) {
    }

    public record VariableGroupView(String key, String label, List<VariableResult> variables) {
    }
}
