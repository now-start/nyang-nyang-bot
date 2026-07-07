package org.nowstart.nyangnyangbot.adapter.in.web.command;

import jakarta.servlet.http.HttpServletResponse;
import io.micrometer.common.util.StringUtils;
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
    public String list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean active,
            Model model
    ) {
        model.addAttribute("commands", commandViews(type, active));
        return COMMAND_LIST_FRAGMENT;
    }

    @GetMapping("/editor")
    public String editor(
            @RequestParam(required = false) Long commandId,
            Model model
    ) {
        model.addAttribute("commandForm", commandId == null ? CommandForm.empty() : commandForm(commandId));
        return COMMAND_EDITOR_FRAGMENT;
    }

    @PostMapping("/validate")
    public String validate(@ModelAttribute CommandForm form, Model model) {
        CommandForm normalizedForm = form.normalizedForType();
        var result = manageCommandUseCase.validate(new ValidateCommand(
                normalizedForm.commandId(),
                normalizedForm.type(),
                normalizedForm.trigger(),
                emptyToNull(normalizedForm.actionKey()),
                normalizedForm.messageTemplate(),
                normalizedForm.timerIntervalMinutes(),
                normalizedForm.timerMinChatCount(),
                normalizedForm.requiredRole(),
                normalizedForm.userCooldownSeconds()
        ));
        model.addAttribute("validationResult", new ValidationView(result.valid(), result.errors()));
        return COMMAND_VALIDATE_FRAGMENT;
    }

    @PostMapping("/preview")
    public String preview(@ModelAttribute CommandForm form, Model model) {
        try {
            var result = manageCommandUseCase.preview(new PreviewCommand(
                    form.messageTemplate(),
                    defaultValue(form.nickname(), "치즈냥"),
                    defaultValue(form.trigger(), "!명령어"),
                    defaultValue(form.args(), ""),
                    firstArg(form.args()),
                    secondArg(form.args()),
                    form.favorite()
            ));
            model.addAttribute("previewMessage", result.message());
        } catch (IllegalArgumentException e) {
            model.addAttribute("previewError", e.getMessage());
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
        CommandForm activeForm = form.withActive(active).normalizedForType();
        try {
            CommandResult result = activeForm.commandId() == null
                    ? manageCommandUseCase.createCommand(createCommand(activeForm, actor(authentication)))
                    : manageCommandUseCase.updateCommand(activeForm.commandId(), updateCommand(activeForm, actor(authentication)));
            model.addAttribute("commandForm", CommandForm.from(result));
            model.addAttribute("saveMessage", "저장됨");
            response.addHeader("HX-Trigger", COMMAND_LIST_REFRESH_TRIGGER);
        } catch (IllegalArgumentException e) {
            model.addAttribute("commandForm", activeForm.withDefaults());
            model.addAttribute("saveError", e.getMessage());
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
        if (form.commandId() == null) {
            model.addAttribute("commandForm", form.withDefaults());
            model.addAttribute("saveError", "저장된 명령어만 비활성화할 수 있습니다.");
            return COMMAND_EDITOR_FRAGMENT;
        }
        CommandForm inactiveForm = form.deactivate().normalizedForType();
        try {
            CommandResult result = manageCommandUseCase.updateCommand(inactiveForm.commandId(), updateCommand(inactiveForm, actor(authentication)));
            model.addAttribute("commandForm", CommandForm.from(result));
            model.addAttribute("saveMessage", "비활성화됨");
            response.addHeader("HX-Trigger", COMMAND_LIST_REFRESH_TRIGGER);
        } catch (IllegalArgumentException e) {
            model.addAttribute("commandForm", inactiveForm.withDefaults());
            model.addAttribute("saveError", e.getMessage());
        }
        return COMMAND_EDITOR_FRAGMENT;
    }

    private List<CommandView> commandViews(String type, Boolean active) {
        return manageCommandUseCase.getCommands().stream()
                .filter(command -> StringUtils.isBlank(type) || type.equals(command.type()))
                .filter(command -> active == null || active == command.active())
                .map(CommandView::from)
                .toList();
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
                form.type(),
                form.trigger(),
                emptyToNull(form.actionKey()),
                form.messageTemplate(),
                form.timerIntervalMinutes(),
                form.timerMinChatCount(),
                form.active(),
                form.requiredRole(),
                form.userCooldownSeconds(),
                actor
        );
    }

    private UpdateCommand updateCommand(CommandForm form, String actor) {
        return new UpdateCommand(
                form.type(),
                form.trigger(),
                emptyToNull(form.actionKey()),
                form.messageTemplate(),
                form.timerIntervalMinutes(),
                form.timerMinChatCount(),
                form.active(),
                form.requiredRole(),
                form.userCooldownSeconds(),
                actor
        );
    }

    private String actor(Authentication authentication) {
        return authentication == null ? DEFAULT_ACTOR : authentication.getName();
    }

    private String emptyToNull(String value) {
        return StringUtils.isBlank(value) ? null : value;
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.isBlank(value) ? fallback : value;
    }

    private String firstArg(String args) {
        String[] values = splitArgs(args);
        return values.length == 0 ? null : values[0];
    }

    private String secondArg(String args) {
        String[] values = splitArgs(args);
        return values.length < 2 ? null : values[1];
    }

    private String[] splitArgs(String args) {
        if (StringUtils.isBlank(args)) {
            return new String[0];
        }
        return args.trim().split("\\s+", 3);
    }

    public record OptionView(String value, String label) {
    }

    public record CommandForm(
            Long commandId,
            String type,
            String trigger,
            String actionKey,
            String messageTemplate,
            Integer timerIntervalMinutes,
            Integer timerMinChatCount,
            Boolean active,
            String requiredRole,
            Integer userCooldownSeconds,
            String nickname,
            String args,
            Integer favorite
    ) {

        static CommandForm empty() {
            return new CommandForm(null, "TEXT", "", "", "", 10, 10, false, "USER", 30, "치즈냥", "첫번째 두번째", 100);
        }

        static CommandForm from(CommandResult result) {
            return new CommandForm(
                    result.id(),
                    result.type(),
                    result.trigger(),
                    result.actionKey(),
                    result.messageTemplate(),
                    result.timerIntervalMinutes(),
                    result.timerMinChatCount(),
                    result.active(),
                    result.requiredRole(),
                    result.userCooldownSeconds(),
                    "치즈냥",
                    "첫번째 두번째",
                    100
            ).withDefaults();
        }

        CommandForm deactivate() {
            return new CommandForm(
                    commandId,
                    type,
                    trigger,
                    actionKey,
                    messageTemplate,
                    timerIntervalMinutes,
                    timerMinChatCount,
                    false,
                    requiredRole,
                    userCooldownSeconds,
                    nickname,
                    args,
                    favorite
            ).withDefaults();
        }

        CommandForm withActive(boolean active) {
            return new CommandForm(
                    commandId,
                    type,
                    trigger,
                    actionKey,
                    messageTemplate,
                    timerIntervalMinutes,
                    timerMinChatCount,
                    active,
                    requiredRole,
                    userCooldownSeconds,
                    nickname,
                    args,
                    favorite
            ).withDefaults();
        }

        CommandForm normalizedForType() {
            CommandForm form = withDefaults();
            if ("TIMER".equals(form.type)) {
                return new CommandForm(
                        form.commandId,
                        form.type,
                        null,
                        null,
                        form.messageTemplate,
                        form.timerIntervalMinutes,
                        form.timerMinChatCount,
                        form.active,
                        form.requiredRole,
                        form.userCooldownSeconds,
                        form.nickname,
                        form.args,
                        form.favorite
                );
            }
            if ("TRIGGER".equals(form.type)) {
                return new CommandForm(
                        form.commandId,
                        form.type,
                        form.trigger,
                        form.actionKey,
                        null,
                        null,
                        null,
                        form.active,
                        form.requiredRole,
                        form.userCooldownSeconds,
                        form.nickname,
                        form.args,
                        form.favorite
                );
            }
            return new CommandForm(
                    form.commandId,
                    form.type,
                    form.trigger,
                    null,
                    form.messageTemplate,
                    null,
                    null,
                    form.active,
                    form.requiredRole,
                    form.userCooldownSeconds,
                    form.nickname,
                    form.args,
                    form.favorite
            );
        }

        CommandForm withDefaults() {
            return new CommandForm(
                    commandId,
                    defaultString(type, "TEXT"),
                    defaultString(trigger, ""),
                    defaultString(actionKey, ""),
                    defaultString(messageTemplate, ""),
                    timerIntervalMinutes == null ? 10 : timerIntervalMinutes,
                    timerMinChatCount == null ? 10 : timerMinChatCount,
                    active != null && active,
                    defaultString(requiredRole, "USER"),
                    userCooldownSeconds == null ? 30 : userCooldownSeconds,
                    defaultString(nickname, "치즈냥"),
                    defaultString(args, "첫번째 두번째"),
                    favorite == null ? 100 : favorite
            );
        }

        private static String defaultString(String value, String fallback) {
            return StringUtils.isBlank(value) ? fallback : value;
        }
    }

    public record CommandView(
            Long id,
            String type,
            String triggerLabel,
            String messageSummary,
            boolean active,
            String statusLabel,
            String cooldownLabel,
            String updatedByLabel
    ) {

        static CommandView from(CommandResult result) {
            String triggerLabel = StringUtils.isBlank(result.trigger()) ? "-" : result.trigger();
            String messageSummary = StringUtils.isBlank(result.messageTemplate()) ? "응답 없음" : result.messageTemplate();
            if (messageSummary.length() > 60) {
                messageSummary = messageSummary.substring(0, 57) + "...";
            }
            return new CommandView(
                    result.id(),
                    result.type(),
                    triggerLabel,
                    messageSummary,
                    result.active(),
                    result.active() ? "활성" : "비활성",
                    result.userCooldownSeconds() == null ? "-" : result.userCooldownSeconds() + "초",
                    defaultString(result.updatedBy(), defaultString(result.createdBy(), "-"))
            );
        }

        private static String defaultString(String value, String fallback) {
            return StringUtils.isBlank(value) ? fallback : value;
        }
    }

    public record ValidationView(boolean valid, List<String> errors) {
    }
}
