package org.nowstart.nyangnyangbot.adapter.in.web.timer;

import static org.assertj.core.api.BDDAssertions.then;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.adapter.in.web.timer.TimerMessageController.TimerMessageForm;
import org.nowstart.nyangnyangbot.adapter.in.web.timer.TimerMessageController.TimerMessageView;
import org.nowstart.nyangnyangbot.adapter.in.web.timer.TimerMessageModelAdvice.OptionView;
import org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.TimerMessageResult;
import org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.VariableResult;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

class TimerMessageTemplateTest {

    @Test
    void timerTemplate_ShouldRenderCompleteManagementFlow() {
        WebContext context = webContext("");
        setVariables(context);

        String html = templateEngine().process("features/timer/regions", context);

        then(html).contains("타이머 메시지");
        then(html).contains("id=\"timer-filter-form\"");
        then(html).contains("id=\"timer-list-region\"");
        then(html).contains("id=\"timer-editor-region\"");
        then(html).contains("새 타이머");
        then(html).contains("타이머 생성");
        then(html).contains("검증 및 미리보기");
        then(html).contains("hx-post=\"/admin/timers/preview\"");
        then(html).contains("id=\"timer-review-region\"");
        then(html).contains("data-review-target=\"#timer-review-region\"");
        then(html).contains("class=\"template-preview-message\"");
        then(html).contains("role=\"status\"");
        then(html).contains("data-template-target=\"#timer-message-template\"");
        then(html).contains("data-template-variable=\"{time.time}\"");
        then(html).contains("마지막 성공 발송 이후");
        then(html).doesNotContain("viewer.nickname");
        then(html).doesNotContain("hx-post=\"/admin/timers/validate\"");
    }

    @Test
    void timerEditor_ShouldDeactivateUsingStoredIdOnly() {
        WebContext context = webContext("");
        setVariables(context);
        context.setVariable("timerMessageForm", new TimerMessageForm(
                1L,
                "현재 시각은 {time.time}입니다.",
                30,
                10,
                true,
                7L,
                "2026-07-16 20:30",
                "2026-07-16 21:30"
        ));

        String html = templateEngine().process("features/timer/regions", context);

        then(html).contains("hx-include=\"#timer-editor-form [name=timerMessageId]\"");
        then(html).contains("비활성화");
    }

    @Test
    void timerTemplate_ShouldRenderContextPathAwareUrls() {
        WebContext context = webContext("/nyang-nyang-bot");
        setVariables(context);

        String html = templateEngine().process("features/timer/regions", context);

        then(html).contains("hx-get=\"/nyang-nyang-bot/admin/timers\"");
        then(html).contains("hx-get=\"/nyang-nyang-bot/admin/timers/editor/new\"");
        then(html).contains("hx-post=\"/nyang-nyang-bot/admin/timers\"");
        then(html).contains("hx-post=\"/nyang-nyang-bot/admin/timers/preview\"");
    }

    private void setVariables(WebContext context) {
        context.setVariable("timerMessageConstraints", new TimerMessageModelAdvice().timerMessageConstraints());
        Instant now = Instant.parse("2026-07-16T12:00:00Z");
        TimerMessageResult result = new TimerMessageResult(
                1L,
                "현재 시각은 {time.time}입니다.",
                30,
                10,
                true,
                7,
                now.minus(Duration.ofMinutes(30)),
                now.plus(Duration.ofMinutes(30)),
                "admin",
                "admin"
        );
        context.setVariable("timerActiveOptions", List.of(
                new OptionView("", "전체 상태"),
                new OptionView("true", "활성"),
                new OptionView("false", "비활성")
        ));
        context.setVariable("timerMessages", List.of(TimerMessageView.from(result)));
        context.setVariable("timerMessageForm", TimerMessageForm.empty());
        context.setVariable("timerVariables", List.of(
                new VariableResult("time.time", "현재 시각", "실행 시점의 시각", "21:00")
        ));
        context.setVariable("saveMessage", "저장됨");
        context.setVariable("timerReview", new TimerMessageController.ReviewView(
                true, List.of(), "현재 시각은 21:00입니다."
        ));
    }

    private WebContext webContext(String contextPath) {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "/points/list");
        request.setContextPath(contextPath);
        MockHttpServletResponse response = new MockHttpServletResponse();
        return new WebContext(
                JakartaServletWebApplication.buildApplication(servletContext).buildExchange(request, response),
                Locale.KOREAN
        );
    }

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        return templateEngine;
    }
}
