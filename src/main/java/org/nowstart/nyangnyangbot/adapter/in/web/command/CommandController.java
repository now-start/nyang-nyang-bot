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
    private static final String COMMAND_VALIDATE_FRAGMENT = "features/command/regions :: command-validation-region";
    private static final String COMMAND_PREVIEW_FRAGMENT = "features/command/regions :: command-preview-region";
    private static final String DEFAULT_ACTOR = "system";
    private static final String COMMAND_LIST_REFRESH_TRIGGER = "command-list-refresh";

    private final ManageCommandUseCase manageCommandUseCase;

    @GetMapping
    public String list(@RequestParam(required = false) Boolean active, Model model) {
        model.addAttribute("commands", commandViews(active));
        return COMMAND_LIST_FRAGMENT;
    }

    @GetMapping("/editor")
    public String editor(@RequestParam(required = false) Long commandId, Model model) {
        model.addAttribute("commandForm", commandId == null ? CommandForm.empty() : commandForm(commandId));
        addCommandVariables(model);
        return COMMAND_EDITOR_FRAGMENT;
    }

    @PostMapping("/validate")
    public String validate(@ModelAttribute CommandForm form, Model model) {
        CommandForm normalizedForm = form.withDefaults();
        var result = manageCommandUseCase.validate(new ValidateCommand(
                normalizedForm.commandId(),
                normalizedForm.trigger(),
                normalizedForm.messageTemplate(),
                normalizedForm.userCooldownSeconds()
        ));
        model.addAttribute("validationResult", new ValidationView(result.valid(), result.errors()));
        return COMMAND_VALIDATE_FRAGMENT;
    }

    @PostMapping("/preview")
    public String preview(@ModelAttribute CommandForm form, Model model) {
        try {
            var result = manageCommandUseCase.preview(new PreviewCommand(form.messageTemplate()));
            model.addAttribute("previewMessage", result.message());
        } catch (IllegalArgumentException | ConstraintViolationException e) {
            model.addAttribute("previewError", inputErrorMessage(e));
        }
        return COMMAND_PREVIEW_FRAGMENT;
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
            @ModelAttribute CommandForm form,
            Authentication authentication,
            HttpServletResponse response,
            Model model
    ) {
        addCommandVariables(model);
        if (form.commandId() == null) {
            model.addAttribute("commandForm", form.withDefaults());
            model.addAttribute("saveError", "저장된 명령어만 비활성화할 수 있습니다.");
            return COMMAND_EDITOR_FRAGMENT;
        }
        CommandForm inactiveForm = form.deactivate();
        try {
            CommandResult result = manageCommandUseCase.updateCommand(
                    inactiveForm.commandId(),
                    updateCommand(inactiveForm, actor(authentication))
            );
            model.addAttribute("commandForm", CommandForm.from(result));
            model.addAttribute("saveMessage", "비활성화됨");
            response.addHeader("HX-Trigger", COMMAND_LIST_REFRESH_TRIGGER);
        } catch (IllegalArgumentException | ConstraintViolationException e) {
            model.addAttribute("commandForm", inactiveForm);
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
        model.addAttribute("commandVariables", manageCommandUseCase.getVariables());
    }

    private String inputErrorMessage(RuntimeException exception) {
        if (exception instanceof ConstraintViolationException violationException) {
            return violationException.getConstraintViolations().stream()
                    .map(violation -> violation.getMessage())
                    .sorted()
                    .distinct()
                    .collect(java.util.stream.Collectors.joining(", "));
        }
        return exception.getMessage();
    }

    private CommandForm commandForm(Long commandId) {
        return manageCommandUseCase.getCommands().stream()
                .filter(command -> commandId.equals(command.id()))
                .findFirst()
                .map(CommandForm::from)
                .orElseGet(CommandForm::empty);
    }

    private CreateCommand createCommand(CommandForm form, String actor) {
        return new CreateCommand(
                form.trigger(),
                form.messageTemplate(),
                form.active(),
                form.userCooldownSeconds(),
                actor
        );
    }

    private UpdateCommand updateCommand(CommandForm form, String actor) {
        return new UpdateCommand(
                form.trigger(),
                form.messageTemplate(),
                form.active(),
                form.userCooldownSeconds(),
                actor
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
            Integer userCooldownSeconds
    ) {

        static CommandForm empty() {
            return new CommandForm(null, "", "", false, 30);
        }

        static CommandForm from(CommandResult result) {
            return new CommandForm(
                    result.id(),
                    result.trigger(),
                    result.messageTemplate(),
                    result.active(),
                    result.userCooldownSeconds()
            ).withDefaults();
        }

        CommandForm deactivate() {
            return new CommandForm(
                    commandId,
                    trigger,
                    messageTemplate,
                    false,
                    userCooldownSeconds
            ).withDefaults();
        }

        CommandForm withActive(boolean active) {
            return new CommandForm(
                    commandId,
                    trigger,
                    messageTemplate,
                    active,
                    userCooldownSeconds
            ).withDefaults();
        }

        CommandForm withDefaults() {
            return new CommandForm(
                    commandId,
                    trigger == null ? "" : trigger,
                    messageTemplate == null ? "" : messageTemplate,
                    active != null && active,
                    userCooldownSeconds == null ? 30 : userCooldownSeconds
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
                    result.userCooldownSeconds() == null ? "-" : result.userCooldownSeconds() + "초",
                    defaultString(result.updatedBy(), defaultString(result.createdBy(), "-"))
            );
        }

        private static String defaultString(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value;
        }
    }

    public record ValidationView(boolean valid, List<String> errors) {
    }
}
