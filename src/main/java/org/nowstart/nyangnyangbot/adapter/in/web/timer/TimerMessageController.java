package org.nowstart.nyangnyangbot.adapter.in.web.timer;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase;
import org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.CreateTimerMessage;
import org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.PreviewTimerMessage;
import org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.TimerMessageResult;
import org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.UpdateTimerMessage;
import org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.ValidateTimerMessage;
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
@RequestMapping("/admin/timers")
@PreAuthorize("hasRole('ADMIN')")
public class TimerMessageController {

    private static final String TIMER_LIST_FRAGMENT = "features/timer/regions :: timer-list-region";
    private static final String TIMER_EDITOR_FRAGMENT = "features/timer/regions :: timer-editor-region";
    private static final String TIMER_REVIEW_FRAGMENT = "features/timer/regions :: timer-review-region";
    private static final String TIMER_LIST_REFRESH_TRIGGER = "timer-list-refresh";
    private static final String DEFAULT_ACTOR = "system";
    private static final String TIMER_NOT_FOUND_MESSAGE = "타이머를 찾을 수 없습니다.";
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ManageTimerMessageUseCase manageTimerMessageUseCase;

    @GetMapping
    public String list(@RequestParam(required = false) Boolean active, Model model) {
        model.addAttribute("timerMessages", manageTimerMessageUseCase.getTimerMessages().stream()
                .filter(timer -> active == null || active == timer.active())
                .map(TimerMessageView::from)
                .toList());
        return TIMER_LIST_FRAGMENT;
    }

    @GetMapping("/editor")
    public String editor(@RequestParam(required = false) Long timerMessageId, Model model) {
        if (timerMessageId != null) {
            try {
                model.addAttribute("timerMessageForm", timerMessageForm(timerMessageId));
                addTimerVariables(model);
            } catch (IllegalArgumentException e) {
                model.addAttribute("saveError", inputErrorMessage(e));
            }
        }
        return TIMER_EDITOR_FRAGMENT;
    }

    @GetMapping("/editor/new")
    public String newEditor(Model model) {
        model.addAttribute("timerMessageForm", TimerMessageForm.empty());
        addTimerVariables(model);
        return TIMER_EDITOR_FRAGMENT;
    }

    @PostMapping("/preview")
    public String preview(@ModelAttribute TimerMessageForm form, Model model) {
        TimerMessageForm normalized = form.withDefaults();
        var validation = manageTimerMessageUseCase.validate(new ValidateTimerMessage(
                normalized.messageTemplate(),
                normalized.intervalMinutes(),
                normalized.minChatCount()
        ));
        if (!validation.valid()) {
            model.addAttribute("timerReview", new ReviewView(false, validation.errors(), null));
            return TIMER_REVIEW_FRAGMENT;
        }
        try {
            var preview = manageTimerMessageUseCase.preview(
                    new PreviewTimerMessage(normalized.messageTemplate())
            );
            model.addAttribute("timerReview", new ReviewView(true, List.of(), preview.message()));
        } catch (IllegalArgumentException | ConstraintViolationException e) {
            model.addAttribute("timerReview", new ReviewView(false, List.of(inputErrorMessage(e)), null));
        }
        return TIMER_REVIEW_FRAGMENT;
    }

    @PostMapping
    public String save(
            @ModelAttribute TimerMessageForm form,
            @RequestParam(defaultValue = "false") boolean active,
            Authentication authentication,
            HttpServletResponse response,
            Model model
    ) {
        TimerMessageForm activeForm = form.withActive(active).withDefaults();
        addTimerVariables(model);
        try {
            TimerMessageResult result = activeForm.timerMessageId() == null
                    ? manageTimerMessageUseCase.createTimerMessage(createTimerMessage(activeForm, actor(authentication)))
                    : manageTimerMessageUseCase.updateTimerMessage(
                            activeForm.timerMessageId(),
                            updateTimerMessage(activeForm, actor(authentication))
                    );
            model.addAttribute("timerMessageForm", TimerMessageForm.from(result));
            model.addAttribute("saveMessage", "저장됨");
            response.addHeader("HX-Trigger", TIMER_LIST_REFRESH_TRIGGER);
        } catch (IllegalArgumentException | ConstraintViolationException e) {
            model.addAttribute("timerMessageForm", activeForm);
            model.addAttribute("saveError", inputErrorMessage(e));
        }
        return TIMER_EDITOR_FRAGMENT;
    }

    @PostMapping("/deactivate")
    public String deactivate(
            @RequestParam(required = false) Long timerMessageId,
            Authentication authentication,
            HttpServletResponse response,
            Model model
    ) {
        addTimerVariables(model);
        if (timerMessageId == null) {
            model.addAttribute("saveError", "저장된 타이머만 비활성화할 수 있습니다.");
            return TIMER_EDITOR_FRAGMENT;
        }
        try {
            TimerMessageResult result = manageTimerMessageUseCase.updateTimerMessage(
                    timerMessageId,
                    new UpdateTimerMessage(null, null, null, false, actor(authentication))
            );
            model.addAttribute("timerMessageForm", TimerMessageForm.from(result));
            model.addAttribute("saveMessage", "비활성화됨");
            response.addHeader("HX-Trigger", TIMER_LIST_REFRESH_TRIGGER);
        } catch (IllegalArgumentException | ConstraintViolationException e) {
            model.addAttribute("saveError", inputErrorMessage(e));
        }
        return TIMER_EDITOR_FRAGMENT;
    }

    private TimerMessageForm timerMessageForm(Long timerMessageId) {
        return manageTimerMessageUseCase.getTimerMessages().stream()
                .filter(timer -> timerMessageId.equals(timer.id()))
                .findFirst()
                .map(TimerMessageForm::from)
                .orElseThrow(() -> new IllegalArgumentException(TIMER_NOT_FOUND_MESSAGE));
    }

    private void addTimerVariables(Model model) {
        model.addAttribute("timerVariables", manageTimerMessageUseCase.getVariables());
    }

    private CreateTimerMessage createTimerMessage(TimerMessageForm form, String actor) {
        return new CreateTimerMessage(
                form.messageTemplate(),
                form.intervalMinutes(),
                form.minChatCount(),
                form.active(),
                actor
        );
    }

    private UpdateTimerMessage updateTimerMessage(TimerMessageForm form, String actor) {
        return new UpdateTimerMessage(
                form.messageTemplate(),
                form.intervalMinutes(),
                form.minChatCount(),
                form.active(),
                actor
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
        return "timer message not found".equals(exception.getMessage())
                ? TIMER_NOT_FOUND_MESSAGE
                : exception.getMessage();
    }

    private String actor(Authentication authentication) {
        return authentication == null ? DEFAULT_ACTOR : authentication.getName();
    }

    public record TimerMessageForm(
            Long timerMessageId,
            String messageTemplate,
            Integer intervalMinutes,
            Integer minChatCount,
            Boolean active,
            Long chatCountSinceLastSend,
            LocalDateTime lastSentAt,
            LocalDateTime nextRunAt
    ) {

        public static TimerMessageForm empty() {
            return new TimerMessageForm(null, "", 30, 10, false, 0L, null, null);
        }

        public static TimerMessageForm from(TimerMessageResult result) {
            return new TimerMessageForm(
                    result.id(),
                    result.messageTemplate(),
                    result.intervalMinutes(),
                    result.minChatCount(),
                    result.active(),
                    result.chatCountSinceLastSend(),
                    result.lastSentAt(),
                    result.nextRunAt()
            ).withDefaults();
        }

        TimerMessageForm withActive(boolean value) {
            return new TimerMessageForm(
                    timerMessageId,
                    messageTemplate,
                    intervalMinutes,
                    minChatCount,
                    value,
                    chatCountSinceLastSend == null ? 0L : chatCountSinceLastSend,
                    lastSentAt,
                    nextRunAt
            );
        }

        TimerMessageForm withDefaults() {
            return new TimerMessageForm(
                    timerMessageId,
                    messageTemplate == null ? "" : messageTemplate,
                    intervalMinutes == null ? 30 : intervalMinutes,
                    minChatCount == null ? 10 : minChatCount,
                    active != null && active,
                    chatCountSinceLastSend == null ? 0L : chatCountSinceLastSend,
                    lastSentAt,
                    nextRunAt
            );
        }
    }

    public record TimerMessageView(
            Long id,
            String messageSummary,
            boolean active,
            String statusLabel,
            String intervalLabel,
            String chatProgressLabel,
            String nextRunLabel,
            String updatedByLabel
    ) {

        public static TimerMessageView from(TimerMessageResult result) {
            String summary = result.messageTemplate() == null || result.messageTemplate().isBlank()
                    ? "메시지 없음"
                    : result.messageTemplate();
            if (summary.length() > 60) {
                summary = summary.substring(0, 57) + "...";
            }
            return new TimerMessageView(
                    result.id(),
                    summary,
                    result.active(),
                    result.active() ? "활성" : "비활성",
                    result.intervalMinutes() + "분",
                    result.chatCountSinceLastSend() + " / " + result.minChatCount(),
                    format(result.nextRunAt()),
                    defaultString(result.updatedBy(), defaultString(result.createdBy(), "-"))
            );
        }

        private static String format(LocalDateTime value) {
            return value == null ? "-" : value.format(DATE_TIME_FORMAT);
        }

        private static String defaultString(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value;
        }
    }

    public record ReviewView(boolean valid, List<String> errors, String previewMessage) {
    }
}
